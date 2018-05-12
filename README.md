# mcp-ilp-sat

## Setup

### Step 1
Add all the JAR files in MCP_Baseline/lib to the classpath.

### Step 2
Install lp_solve. Follow the instructions on section 2 of this page: http://lpsolve.sourceforge.net/5.5/Java/README.html. The relevant downloads can be found here: https://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.5/. We'll have to install more ILP solvers as we go.

### Step 3
Extract the data from `CNN_DailyMail_10k.rar` and place it in `/data/CNN_DailyMail_10k/`. This will be our test set to see how quickly we can generate summaries. I've also included `CNN_DailyMail_1k.zip`, which can be used if the larger test set takes much too long.