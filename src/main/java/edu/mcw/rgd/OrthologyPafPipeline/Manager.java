package edu.mcw.rgd.OrthologyPafPipeline;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.OrthologDAO;
import edu.mcw.rgd.datamodel.MappedOrtholog;
import edu.mcw.rgd.datamodel.Ortholog;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import edu.mcw.rgd.process.mapping.MapManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
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




        LinkedHashMap<String, Integer> assemblies = new LinkedHashMap<String, Integer>();
        assemblies.put("mRatBN7.2",372);
        assemblies.put("Rnor_6.0",360);
        assemblies.put("Rnor_5.0",70);
        assemblies.put("RGSC_v3.4",60);
        assemblies.put("GRCh38.p14",38);
        assemblies.put("GRCh37.p13",17);
        assemblies.put("NCBI36",13);
        assemblies.put("GRCm39",239);
        assemblies.put("GRCm38",35);
        assemblies.put("MGSCv37",18);
        assemblies.put("CanFam3.1",631);
        assemblies.put("Sscrofa11.1",911);
        assemblies.put("Sscrofa10.2",910);
        assemblies.put("Chlorocebus_sabeus 1.1",1311);
        assemblies.put("Vero_WHO_p1.0",1313);
        assemblies.put("Mhudiblu_PPA_v0",513);
        assemblies.put("PanPan1.1",511);
        assemblies.put("HetGla_female_1.0",1410);
        assemblies.put("ChiLan1.0",44);
        assemblies.put("SpeTri2.0",720);


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

        HashMap<Integer,Boolean> processed = new HashMap<Integer,Boolean>();

        try {
            for (Map.Entry<String, Integer> entry: assemblies.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();

                for (Map.Entry<String,Integer> entry2: assemblies.entrySet()) {

                    String key2 = entry2.getKey();
                    Integer value2 = entry2.getValue();

                    //if (!key.equals(key2)) {
                    if (MapManager.getInstance().getMap(value).getSpeciesTypeKey() != MapManager.getInstance().getMap(value2).getSpeciesTypeKey()) {

                        if (!processed.containsKey(value2)) {
                            manager.run(key, value, key2, value2, outputDirectory);
                        }
                    }

                }
                processed.put(value,true);

//                assemblies.remove(key);

            }


               // manager.run(assembly1, mapKey1, assembly2, mapKey2, outputDirectory);
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

        List<MappedOrtholog> ortho = odao.getAllMappedOrthologs(MapManager.getInstance().getMap(mapKey1).getSpeciesTypeKey(),MapManager.getInstance().getMap(mapKey2).getSpeciesTypeKey(),mapKey1,mapKey2);

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

String orthologyTemplate = """
        {
            "type":"SyntenyTrack",
                "trackId":"assembly12assembly2.anchors",
                "name":"assembly1-assembly2 orthology",
                "assemblyNames": [
                    "assembly1",
                    "assembly2"
            ],
            "category": [
                "Orthologs"
            ],
            "adapter":{
            "type":"MCScanAnchorsAdapter",
                    "mcscanAnchorsLocation":{
                    "locationType":"UriLocation",
                    "uri":"assembly1-assembly2.anchors"
            },
            "bed1Location":{
                "locationType":"UriLocation",
                        "uri":"assembly1/assembly1.bed"
            },
            "bed2Location":{
                "locationType":"UriLocation",
                        "uri":"assembly1/assembly2.bed"
            },
            "assemblyNames": [
                "assembly1",
                "assembly2"
            ]
            },
            "displays": [
            {
                "type":"DotplotDisplay",
                    "displayId":"assembly12assmbly2.anchors-DotplotDisplay"
            },
            {
                "type":"LinearComparativeDisplay",
                    "displayId":"assembly12assembly2.anchors-LinearComparativeDisplay"
            },
            {
                "type":"LinearSyntenyDisplay",
                    "displayId":"assembly12assembly2.anchors-LinearSyntenyDisplay"
            },
            {
                "type":"LGVSyntenyDisplay",
                    "displayId":"assembly12assembly2.anchors-LGVSyntenyDisplay",
                    "mouseover":"jexl:get(feature,'mate').name"
            }
        ]
        }

""";

    orthologyTemplate = orthologyTemplate.replaceAll("assembly1",assembly1);
        orthologyTemplate = orthologyTemplate.replaceAll("assembly2",assembly2);
        fw = new FileWriter(new File(outputDirectory + "/" + assembly1 + "-" + assembly2 +".json"));
        fw.write(orthologyTemplate);
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
