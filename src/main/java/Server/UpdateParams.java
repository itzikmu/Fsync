package Server;

import name.pachler.nio.file.Path;

public class UpdateParams {

    public  int numOfClientsNotUpdated;
    public  String updateType;
    public  Path fileToDelete;
    public  Path fileToRenameFrom;
    public  Path getFileToRenameTo;

    public UpdateParams(){
        resetParams();
    }

    public  synchronized void decreaseNotUpdatedClientsCounter(){
        numOfClientsNotUpdated--;
    }
    public  synchronized void setNumOfClientsToUpdate(int num){
        numOfClientsNotUpdated = num;
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
