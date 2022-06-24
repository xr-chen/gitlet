package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Xingrong Chen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public static void checkExist() {
        if (!Repository.isRepositoryDir()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        // If args are empty
        if (args.length == 0) {
            System.out.println("Pleas enter a command");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.creatRepository();
                break;
            case "add":
                checkExist();
                // check if we are in a initialized Gitlet repository
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
                    System.out.println("Please enter a file name");
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            case "rm":
                checkExist();
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
                    System.out.println("Please enter a file name");
                    System.exit(0);
                }
                Repository.rm(args[1]);
                break;
            case "commit":
                checkExist();
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "log":
                checkExist();
                Repository.printLog();
                break;
            case "status":
                checkExist();
                Repository.printStatus();
                break;
            case "test":
                Repository.test();
                break;
            case "checkout":
                if (args.length <= 1 || args[1] == null ||  args[1].equals("")) {
                    System.out.println("Please enter a file name to checkout");
                    System.exit(0);
                }
                if (args.length == 2) {
                    Repository.checkOutBranch(args[1]);
                } else if (args[1].equals("--") && args.length == 3) {
                    Repository.checkoutFileInCommit(args[2], Branch.getHeadId());
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutFileInCommit(args[3], args[1]);
                } else {
                    System.out.println("Wrong arguments,");
                    System.out.println("    java gitlet.Main checkout -- [file name]");
                    System.out.println("    java gitlet.Main checkout [commit id] -- [file name]");
                    System.out.println("    java gitlet.Main checkout [branch name]");
                }
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.findWithMsg(args[1]);
                break;
            default:
                System.out.println("No command with that name exists");
                System.exit(0);
        }

    }
}
