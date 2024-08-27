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

        String assembly1 = args[0];
        int mapKey1 = Integer.parseInt(args[1]);
        String assembly2 = args[2];
        int mapKey2 = Integer.parseInt(args[3]);
        String outputDirectory = args[4];

        Date time0 = Calendar.getInstance().getTime();

        try {
                manager.run(assembly1, mapKey1, assembly2, mapKey2, outputDirectory);
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

    public void run(String assembly1, int mapKey1, String assembly2, int mapKey2, String outputDirectory) throws Exception {

        MapDAO mdao = new MapDAO();

        Map<String,Integer> chrLen1 = mdao.getChromosomeSizes(mapKey1);
        Map<String,Integer> chrLen2 = mdao.getChromosomeSizes(mapKey2);

        OrthologDAO odao = new OrthologDAO();
        List<MappedOrtholog> ortho = odao.getAllMappedOrthologs(3,1,mapKey1,mapKey2);

        FileWriter fw = new FileWriter(new File(outputDirectory + "/" + assembly1 +".bed"));
        for (MappedOrtholog mo: ortho) {
            String row = "";
            row += "Chr" + mo.getSrcChromosome() + "\t";
            row += mo.getSrcStartPos() + "\t";
            row += mo.getSrcStopPos() + "\t";
            row += mo.getSrcGeneSymbol() + "\t";
            row += "0\t";
            row += mo.getSrcStrand();
            fw.write(row + "\n");
        }
        fw.close();

        fw = new FileWriter(new File(outputDirectory + "/" + assembly2 +".bed"));
        for (MappedOrtholog mo: ortho) {
            String row = "";
            row += "Chr" + mo.getDestChromosome() + "\t";
            row += mo.getDestStartPos() + "\t";
            row += mo.getDestStopPos() + "\t";
            row += mo.getDestGeneSymbol() + "\t";
            row += "0\t";
            row += mo.getDestStrand();
            fw.write(row + "\n");
        }
        fw.close();

        fw = new FileWriter(new File(outputDirectory + "/" + assembly1 + "-" + assembly2 +".anchors"));
        for (MappedOrtholog mo: ortho) {
            String row = "";
            row += mo.getSrcGeneSymbol() + "\t";
            row += mo.getDestGeneSymbol() + "\t";
            row += "100\t";
            fw.write(row + "\n");
        }
        fw.close();

    }



    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
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
