package edu.mcw.rgd.OrthologyPafPipeline;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.OrthologDAO;
import edu.mcw.rgd.datamodel.MappedGene;
import edu.mcw.rgd.datamodel.MappedOrtholog;

import java.util.List;

/**
 * @author cdursun
 * @since 2/3/2017
 * wrapper for all calls to database
 */
public class Dao {

    private OrthologDAO orthologDAO = new OrthologDAO();
    private GeneDAO geneDAO = new GeneDAO();

    public List<MappedGene> getActiveMappedGenes(int mapKey) throws Exception {
        return geneDAO.getActiveMappedGenes(mapKey);
    }

    public List<MappedOrtholog> getAllMappedOrthologs(int speciesKey1, int speciesKey2, int mapKey1, int mapKey2) throws Exception {
        return orthologDAO.getAllMappedOrthologs(speciesKey1, speciesKey2, mapKey1, mapKey2);
    }
}
