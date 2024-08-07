package edu.mcw.rgd.OrthologyPafPipeline;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.OrthologDAO;
import edu.mcw.rgd.datamodel.MappedOrtholog;
import edu.mcw.rgd.datamodel.Ortholog;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by cdursun on 2/3/2017.
 */
public class Manager {

    public static final int INSERT_COUNTER = 0;
    public static final int UPDATE_COUNTER = 1;

    private String version;
    Logger logger = LogManager.getLogger("status");
    private int transitiveOrthologPipelineId;
    private String xrefDataSrc;
    private String xrefDataSet;
    private int transitiveOrthologType;
    private Dao dao;

    /**
     * load spring configuration from properties/AppConfigure.xml file
     * and run the pipeline
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager = (Manager) bf.getBean("main");

        manager.logger.info(manager.getVersion());


        if( args == null || args.length < 2 ){
            System.out.println("");
            System.out.println("            Missing parameter!                  ");
            System.out.println("----------- Run with subject species type KEY other than Human! -----------");
            System.exit(0);
        }

        int mapKey1 = SpeciesType.parse(args[0]);
        int mapKey2 = SpeciesType.parse(args[1]);

        Date time0 = Calendar.getInstance().getTime();

        try {
                manager.run(mapKey1, mapKey2);
        } catch(Exception e) {
            Utils.printStackTrace(e, manager.logger);
            throw e;
        }

        manager.logger.info("=== OK === elapsed time " + Utils.formatElapsedTime(time0.getTime(), System.currentTimeMillis()));
        manager.logger.info("");
    }

    /*
    Col	Type	Description
1	string	Query sequence name
2	int	Query sequence length
3	int	Query start (0-based; BED-like; closed)
4	int	Query end (0-based; BED-like; open)
5	char	Relative strand: "+" or "-"
6	string	Target sequence name
7	int	Target sequence length
8	int	Target start on original strand (0-based)
9	int	Target end on original strand (0-based)
10	int	Number of residue matches
11	int	Alignment block length
12	int	Mapping quality (0-255; 255 for missing)

     */

    public void run(int mapKey1, int mapKey2) throws Exception {

        MapDAO mdao = new MapDAO();

        Map<String,Integer> chrLen1 = mdao.getChromosomeSizes(372);
        Map<String,Integer> chrLen2 = mdao.getChromosomeSizes(38);

        OrthologDAO odao = new OrthologDAO();
        List<MappedOrtholog> ortho = odao.getAllMappedOrthologs(3,1,372,38);
        FileWriter fw = new FileWriter(new File("/Users/jdepons/tmp/dump.out"));

        for (MappedOrtholog mo: ortho) {

            String row = "";

            row += "chr" + mo.getSrcChromosome() + "\t";
            row += chrLen1.get(mo.getSrcChromosome()) + "\t";
            row += mo.getSrcStartPos() + "\t";
            row += mo.getSrcStopPos() + "\t";

            if (mo.getSrcStrand().equals(mo.getDestStrand())) {
                row += "+" + "\t";
            }else {
                row += "-" + "\t";
            }

            row += "chr" + mo.getDestChromosome() + "\t";
            row += chrLen1.get(mo.getDestChromosome()) + ",\t";
            row += mo.getDestStartPos() + "\t";
            row += mo.getDestStopPos() + "\t";

            long residueMatches = mo.getSrcStopPos() - mo.getSrcStartPos();

            row += residueMatches + "\t";
            row += residueMatches + "\t";
            row += "255";
            System.out.println(row);

            fw.write(row + "\n");

        }
        fw.close();

    }

        /**
         * print connection information, download the genes-diseases file from CTD, parse it, QC it and load the annotations into RGD
         * @throws Exception
         */
    public void run(int speciesTypeKey, Date runDate) throws Exception {

        dao.init(runDate, this.transitiveOrthologType, this.transitiveOrthologPipelineId, speciesTypeKey);
        Process process = new Process(runDate, this.transitiveOrthologType, this.transitiveOrthologPipelineId, this.xrefDataSrc, this.xrefDataSet);

        //get subject species human orthologs
        //get human -other species orthologs for the listed subject species ortholog destination human genes
        //  if subject species doesn't have transitive ortholog in the "subject species - human - other species" link
        //        add new two reciprocal transitive orthologs
        //  else if subject species has transitive ortholog for that species and don't have other ortholog types
        //        update last modified date for reciprocal orthologs
        // delete all transitive orthologs that don't have the current last modified date

        List<Ortholog> subjectSpeciesHumanOrthologs = dao.getSubjectSpeciesHumanOrthologs();

        logger.info("");
        logger.info("Orthologs between " + SpeciesType.getCommonName(speciesTypeKey) + " and Human : " + subjectSpeciesHumanOrthologs.size());

        AtomicInteger[] counters = new AtomicInteger[2];
        for( int i=0; i<counters.length; i++ ) {
            counters[i] = new AtomicInteger(0);
        }

        subjectSpeciesHumanOrthologs.parallelStream().forEach( sSHO -> {

            try {
                // get human-other species orthologs
                List<Ortholog> humanOtherSpeciesOrthologs = dao.getHumanOtherSpeciesOrthologs(sSHO.getDestRgdId());
                for (Ortholog hOSO : humanOtherSpeciesOrthologs) {

                    // get subject species - other species orthologs through subject species - human orthologs information
                    // this list consists of transitive ortholog candidates
                    List<Ortholog> subjectSpeciesOtherSpeciesOrthologs = dao.getOrthologs(sSHO, hOSO);

                    // if there is not any subject species-other species ortholog then creates new reciprocal transitive orthologs
                    if (subjectSpeciesOtherSpeciesOrthologs.size() == 0) {
                        dao.insertOrthologs(process.createReciprocalTransitiveOrthologs(sSHO.getSrcRgdId(), hOSO.getDestRgdId()));
                        counters[INSERT_COUNTER].getAndAdd(2);
                    }
                    // if there is not any non-transitive ortholog between the subject species and other species
                    // update last modified dates of the transitive orthologs
                    else if (!process.haveNonTransitiveOrthologs(subjectSpeciesOtherSpeciesOrthologs)) {
                        //if the transitive ortholog is created before this run then we want to update it otherwise no need to
                        dao.updateLastModified(subjectSpeciesOtherSpeciesOrthologs);
                        counters[UPDATE_COUNTER].getAndAdd(2);
                    }
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        logger.info("Updated reciprocal transitive orthologs : " + counters[UPDATE_COUNTER]);
        logger.info("Created reciprocal transitive orthologs : " + counters[INSERT_COUNTER]);

        // finally delete the untouched transitive orthologs after newly introduced genuine orthologs from other sources
        logger.info("Deleted unmodified transitive orthologs : " + dao.deleteUnmodifiedTransitiveOrthologs());
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setTransitiveOrthologPipelineId(int transitiveOrthologPipelineId) {
        this.transitiveOrthologPipelineId = transitiveOrthologPipelineId;
    }

    public int getTransitiveOrthologPipelineId() {
        return transitiveOrthologPipelineId;
    }

    public void setXrefDataSrc(String xrefDataSrc) {
        this.xrefDataSrc = xrefDataSrc;
    }

    public String getXrefDataSrc() {
        return xrefDataSrc;
    }

    public void setXrefDataSet(String xrefDataSet) {
        this.xrefDataSet = xrefDataSet;
    }

    public String getXrefDataSet() {
        return xrefDataSet;
    }

    public void setTransitiveOrthologType(int transitiveOrthologType) {
        this.transitiveOrthologType = transitiveOrthologType;
    }

    public int getTransitiveOrthologType() {
        return transitiveOrthologType;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }
}
