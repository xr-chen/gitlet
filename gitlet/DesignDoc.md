# Gitlet Design Document

**Name**:

## Classes and Data Structures

### Class 1

#### Fields

1. Field 1
2. Field 2


### Class 2

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence

## TODO: 

- [ ] add: The file will no longer be staged for removal (see gitlet rm), if itwas at the time of the command.
- [ ] commit : By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; it will keep versions of files exactly as they are, and not update them.
- [ ] commit : For example, if you remove a tracked file using the Unix rm command (rather than Gitlet’s command of the same name), it has no effect on the next commit, which will still contain the (now deleted) version of the file.
- [ ] commit : Finally, files tracked in the current commit may be untracked in the new commit as a result being staged for removal by the rm command (below).
- [ ] blobs : only use the file name to identify which are committed file, which are staged files
- [ ] log : log for merged branch
- [ ] branch : implement a class for storing heads of each branches.
- [ ] status : The final category (“Untracked Files”) is for files present in the working directory but neither staged for addition nor tracked. This includes files that have been staged for removal, but then re-created without Gitlet’s knowledge.
- [ ] branch : probably affect the function of checkout, commit