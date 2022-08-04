# Gitlet

A git-like version control tool written in java.

## Compile Source Code

Before using Gitlet, we need to compile all java files in `gitlet` folder with command
```
javac gitlet/*.java
```

## Commands

Initialize a new Gitlet version-control system in the current directory
```
java gitlet.Main init
```
Staging the specified file, if the file has been staged, the command will overwrite previous entry in staging area, if the file is identical to the version in the latest commit, the file will be removed from staging area.
```
java gitlet.Main add [file name]
```
Commit changes (addition, removal, update) to files be staged
```
java gitlet.Main commit [message]
```
Unstage the file if it is currently staged for addition, or stage tracked file for removal and remove the file from the working directory
```
java gitlet.Main rm [file name]
```
Display information about each commit backwards along the commit tree until the initial commit
```
java gitlet.Main log
```
Display information about all commits ever made
```
java gitlet.Main global-log
```
Prints out the SHA-1 of all commits with specified message
```
java gitlet.Main find [commit message]
```
Display all existed branches, the current branch was marked with *, files have been staged for addition or removal, and untracked changes.
``` 
java gitlet.Main status
```
Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there
```
java gitlet.Main checkout -- [file name]
```
Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that’s already there
```
java gitlet.Main checkout [commit id] -- [file name]
```
Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there
```
java gitlet.Main checkout [branch name]
```
Creates a new branch with the given name, and points it at the current head commit.
```text
java gitlet.Main branch [branch name]
```
Deletes the branch with the given name.
```text
java gitlet.Main rm-branch [branch name]
```
Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node
```text
java gitlet.Main reset [commit id]
```
Merges files from the given branch into the current branch
```text
java gitlet.Main merge [branch name]
```
