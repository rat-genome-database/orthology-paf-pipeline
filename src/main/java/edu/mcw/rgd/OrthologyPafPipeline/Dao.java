package edu.mcw.rgd.OrthologyPafPipeline;

import edu.mcw.rgd.dao.impl.OrthologDAO;
import edu.mcw.rgd.datamodel.Ortholog;
import edu.mcw.rgd.datamodel.SpeciesType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * @author cdursun
 * @since 2/3/2017
 * wrapper for all calls to database
 */
public class Dao {

    private final static Logger logInserted = LogManager.getLogger("inserted");
    private final static Logger logDeleted = LogManager.getLogger("deleted");

    private Date runDate;
    private int transitiveOrthologType;
    private int transitiveOrthologPipelineId;
    private int subjectSpeciesType;
    private int decisionDurationInMinForOrthologDeletion;


    public void init(Date runDate, int transitiveOrthologType, int transitiveOrthologPipelineId, int subjectSpeciesType){
        this.runDate = runDate;
        this.transitiveOrthologType = transitiveOrthologType;
        this.transitiveOrthologPipelineId = transitiveOrthologPipelineId;
        this.subjectSpeciesType = subjectSpeciesType;
    }

    private OrthologDAO orthologDAO = new OrthologDAO();

    public List<Ortholog> getSubjectSpeciesHumanOrthologs() throws Exception{
        return orthologDAO.getAllOrthologs(this.subjectSpeciesType, SpeciesType.HUMAN);
    }

    /**
     * returns two orthologs, including the reciprocal one between srcRgdId and destRgdId
    */
    public List<Ortholog> getOrthologs(Ortholog srcOrtho, Ortholog humanOrtho) throws Exception{
        List<Ortholog> srcOrthologs = orthologDAO.getOrthologsForSourceRgdId(srcOrtho.getSrcRgdId());
        srcOrthologs.removeIf(o -> o.getDestSpeciesTypeKey() != humanOrtho.getDestSpeciesTypeKey());

        List<Ortholog> destOrthologs = orthologDAO.getOrthologsForSourceRgdId(humanOrtho.getDestRgdId());
        destOrthologs.removeIf(o -> o.getDestSpeciesTypeKey() != srcOrtho.getSrcSpeciesTypeKey());

        srcOrthologs.addAll(destOrthologs);
        return srcOrthologs;
    }

    public int updateLastModified(List<Ortholog> orthologs) throws Exception {
        return orthologDAO.updateLastModified(orthologs, this.transitiveOrthologPipelineId);
    }

    public int insertOrthologs(List<Ortholog> orthologs) throws Exception {
        for( Ortholog o: orthologs ) {
            logInserted.info(o.dump("|"));
        }
        return orthologDAO.insertOrthologs(orthologs);
    }

    public int deleteOrthologs(List<Ortholog> orthologs) throws Exception {
        for( Ortholog o: orthologs ) {
            logDeleted.info(o.dump("|"));
        }
        return orthologDAO.deleteOrthologs(orthologs);
    }

    /**
     * get <b>transitive</b> orthologs for given pair of species that were modified before the run date
     *
     * @return
     * @throws Exception
     */

    public List<Ortholog> getUnmodifiedTransitiveOrthologsSince(int min ) throws Exception {
        // calls getOrthologsModifiedBefore method by subtracting 5 minutes from runDate
        // otherwise newly updated and inserted orthologs are returned which we don't want
        List<Ortholog> unmodifiedOrthologs  = orthologDAO.getOrthologsModifiedBefore(new Date(this.runDate.getTime() - (min * 1000 * 60) ));

        // remove the non-transitive orthologs and the transitive orthologs aren't related with this subject type
        unmodifiedOrthologs.removeIf(o -> o.getOrthologTypeKey() != this.transitiveOrthologType
                || (o.getSrcSpeciesTypeKey() != this.subjectSpeciesType && o.getDestSpeciesTypeKey() != this.subjectSpeciesType));

        return unmodifiedOrthologs;
    }

    /**
     * returns the orthologs of the human @rgdId and all other species types except subjectSpeciesType
     *
     * @param rgdId
     * @return
     */
    public List<Ortholog> getHumanOtherSpeciesOrthologs(int rgdId) throws Exception{

        List<Ortholog> humanOrthologs = orthologDAO.getOrthologsForSourceRgdId(rgdId);

        // remove human-exludingSpeciesTypeKey orthologs
        humanOrthologs.removeIf(o -> o.getDestSpeciesTypeKey() == this.subjectSpeciesType);

        return humanOrthologs;
    }

    /**
     * delete the subjectSpeciesType - Human transitive orthologs before the run date
     *
     * @return
     * @throws Exception
     */
    public int deleteUnmodifiedTransitiveOrthologs() throws Exception{
        List<Ortholog> unmodifiedTransitiveOrthologs = getUnmodifiedTransitiveOrthologsSince(decisionDurationInMinForOrthologDeletion);
        deleteOrthologs(unmodifiedTransitiveOrthologs);
        return unmodifiedTransitiveOrthologs.size();
    }

    public void setDecisionDurationInMinForOrthologDeletion(int decisionDurationInMinForOrthologDeletion) {
        this.decisionDurationInMinForOrthologDeletion = decisionDurationInMinForOrthologDeletion;
    }

    public int getDecisionDurationInMinForOrthologDeletion() {
        return decisionDurationInMinForOrthologDeletion;
    }
}

