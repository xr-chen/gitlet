package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Xingrong Chen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // If args are empty
        if (args.length == 0) {
            System.out.println("Invalid number of arguments, pleas enter a command");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":

                Repository.creatRepository();
                break;
            case "add":
                String fileName = args[1];
                // TODO : implement another method in Repository to support add . command
                Repository.addFiles(fileName);
                break;
            case "test":
                Repository.test();
                break;
            case "commit":
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            default:
                System.out.println("No command with that name exists");
                System.exit(0);
        }


    }
}
