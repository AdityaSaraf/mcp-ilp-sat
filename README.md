# mcp-ilp-sat

## Setup

### Step 1
Add all the JAR files in MCP_Baseline/lib to the classpath. It's highly recommended to use an IDE (both Intellij and Netbeans were used during development) to run this project.

### Step 2
Download GLPK. On Windows, you can do so here: https://sourceforge.net/projects/winglpk/. Get the DLLs from /w64, and copy them to your Windows/System32 directory. The directions may be a bit different on Linux (you can also add the library files to the library path instead of copying to Sys32).

### Step 3
Extract the data from `CNN_DailyMail_10k.rar` and place it in `/data/CNN_DailyMail_10k/`. This will be our test set to see how quickly we can generate summaries. I've also included `CNN_DailyMail_1k.zip`, which can be used if the larger test set takes much too long. There are also other datasets -- the Australian law dataset and a couple of large files.

### Step 4
Download Z3. Set the absolute path to the Z3 bin on your system at Main.java:72.

## Running the Application
You should be able to run the application after following the above set up steps. The application will generate MaxSAT encodings in the main directory and then run Z3 on them. On Main.java:100-102, you can change the encoding to either the Z3 ILP encoding or the GLPK ILP solution. In MCP_MaxSAT, you can change some features of the MaxSAT encoding. For instance, on line 76-77, you can switch from using an integer constraint to using an addition circuit. You can also generate wncf files by uncommenting lines 111-139 (and by using the addition circuit, not the integer constraint).
