package gitlet;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Xingrong Chen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Pleas enter a command");
            System.exit(0);
        }

        String firstArg = args[0];
        checkExist(firstArg);
        switch (firstArg) {
            case "init" -> Repository.creatRepository();
            case "add" -> {
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
                    System.out.println("Please enter a file name");
                    System.exit(0);
                }
                Repository.add(args);
            }
            case "rm" -> {
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
                    System.out.println("Please enter a file name");
                    System.exit(0);
                }
                Repository.rm(args[1]);
            }
            case "commit" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.commit(args[1]);
            }
            case "log" -> Repository.printLog();
            case "status" -> Repository.printStatus();
            case "checkout" -> {
                if (args.length <= 1 || args[1] == null || args[1].equals("")) {
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
            }
            case "global-log" -> Repository.globalLog();
            case "find" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.findWithMsg(args[1]);
            }
            case "branch" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a branch name");
                    System.exit(0);
                }
                Repository.branch(args[1]);
            }
            case "rm-branch" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a branch name");
                    System.exit(0);
                }
                Repository.removeBranch(args[1]);
            }
            case "reset" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a commit id.");
                    System.exit(0);
                }
                Repository.reset(args[1]);
            }
            case "merge" -> {
                if (args.length <= 1 || args[1] == null || args[1].isEmpty()) {
                    System.out.println("Please enter a branch name");
                    System.exit(0);
                }
                Repository.merge(args[1]);
            }
            default -> {
                System.out.println("No command with that name exists");
                System.exit(0);
            }
        }
    }

    private static void checkExist(String arg) {
        if (!arg.equals("init") && !Repository.isRepositoryDir()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(1);
        }
    }

}
