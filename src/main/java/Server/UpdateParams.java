package Server;

import java.util.ArrayList;
import java.util.List;

public class UpdateParams {

    public  int numOfClientsNotUpdated;
    public  String updateType;
    public  List<String> filesToDelete;
    public  String fileToRenameFrom;
    public  String getFileToRenameTo;

    public UpdateParams(){
        resetParams();
    }

    public  synchronized void decreaseNotUpdatedClientsCounter(){
        numOfClientsNotUpdated--;
    }

    public  synchronized void setNumOfClientsToUpdate(int num){
        numOfClientsNotUpdated = num;
    }

    public  synchronized void GetParamsUpdate(UpdateParams newParams){
        if(newParams != null)
        {
            updateType = newParams.updateType;

            if(newParams.updateType == "RENAME")
            {
                fileToRenameFrom = newParams.fileToRenameFrom;
                getFileToRenameTo = newParams.getFileToRenameTo;
            }
            else if (newParams.updateType == "DELETE")
            {
                filesToDelete = newParams.filesToDelete;
            }
        }

    }



    public synchronized void resetParams()
    {
        numOfClientsNotUpdated = 0;
        updateType = "";
        filesToDelete = new ArrayList<String>();
        fileToRenameFrom= null;
        getFileToRenameTo= null;

    }
}
