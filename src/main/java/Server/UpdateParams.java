package Server;

import name.pachler.nio.file.Path;

public class UpdateParams {

    public  int numOfClientsNotUpdated;
    public  String updateType;
    public  String fileToDelete;
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
                fileToDelete = newParams.fileToDelete;
            }
        }

    }



    public synchronized void resetParams()
    {
        numOfClientsNotUpdated = 0;
        updateType = "";
        fileToDelete = null;
        fileToRenameFrom= null;
        getFileToRenameTo= null;

    }
}
