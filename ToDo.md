# To-Do List

1. Gather data
    * Short documents: Get the CNN/DailyMail short articles (**DONE**)
    * Medium documents: Get the Australian Law cases, process the data into a list of `LabelledDocument`'s
    * Long documents: Get large texts, no need for attached summaries, process the data into a list of `LabelledDocument`'s with the following fields initialized: `fileName`, `originalSentences`, `filteredSentences`, and `vocabulary`.
2. Test runtime with multiple ILP solvers. (To do a sanity check, make sure that all solvers have the same value for the objective function).
    * Set up the MCP to ILP encoding through `JavaILP` (**DONE**)
    * Test the data sets on multiple ILP solvers: lp_solve (**DONE**), Gurobi, GLPK
3. Test with at least one Partial-Weighted MaxSAT solver. (To do a sanity check, make sure the solution has the same optimal value as the ILP solvers).
    * Think of *multiple* MCP to MaxSAT encodings. Set up the encoding through the solver.
    * Test the data sets on the solver.

Stretch Goals:
1. Easy: Try more weighted MCP as well.
2. Medium: Try more Partial-Weighted MaxSAT solvers if time allows (might be challenging if the other solvers don't have clean interfaces).
3. Hard: Try different formulations of text summarization! E.g. Redundancy-Constrained Knapsack, anaphora/compression, etc.