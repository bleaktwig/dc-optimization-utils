# DC OPTIMIZATION UTILITIES
Container for various utility files to be used for the "Forward Tracking code speed optimization" service works.

## Setup.
Using these utilities requires a [coatjava](https://github.com/JeffersonLab/clas12-offline-software) installation.
Refer to its repository for installation instructions.

You are encouraged to add `/path/to/coatjava/bin` to your `PATH` variable for ease of access.

The files to be used for benchmarking are:
* sidis_vz0.hipo: gemc simulation with 10.000 events for quick comparison.
Stored at `/work/clas12/users/devita/clas12validation/infiles/sidis/sidis_vz0.hipo`.
* clas_005038.1231.hipo: data file with 70.000 events for serious resolution tests.
Stored at `/work/clas12/users/devita/clas12validation/infiles/clas_005038.1231.hipo`.

## Profiling.
To profile code and find HotSpots use the `visualvm` tool installed in the benchmarking computer on the data file.
Remember to not run anything else while running reconstruction in this machine to not spoil results.
A screenshot of profiling results at different dates is included in the `prof_results` directory for future reference.

## Speed Testing.
To compare the code speed of different versions simply run reconstruction in the benchmarking computer on the data file.
At the end of reconstruction a solid summary of per-engine running time is printed, which is good enough for comparison.

## The Plan.
Optimization work consists of three fields.
These are detailed in the following subsections.

### Java versions profiling and comparison
* Adapt branch of offline reconstruction software to work with OpenJDK 17.
* Make sure that reconstruction results are the same using `compareBanks.groovy`.
* Profile and compare speed from both branches.
* See if there's any obvious improvement to be found by using new methods from Java 17.

### Refactoring and optimization of DC tracking
* Refactor and optimize HotSpot methods:
    * `cnuphys.magfield.FieldProbe.contains()`                                (23.2%)
    * `org.jlab.rec.dc.track.RungeKuttaDoca.RK4transport()`                   (21.4%)
    * `cnuphys.magield.RotatedCompositeProbe.field()`                         (11.5%)
    * `org.jlab.rec.dc.cluster.ClusterCleanerUtilities.ClusterSplitter()`     ( 6.9%)
    * `org.jlab.rec.dc.cluster.ClusterCleanerUtilities.LRAmbiguityResolver()` ( 6.3%)
    * `cnuphys.magield.CompositeProbe.field()`                                ( 5.8%)
* Make sure that reconstruction results are the same via `compareBanks.groovy`.
* Refactor the whole DC codebase and document.
* See if any further optimization can be done on the new HotSpot methods.

### Apply algorithmical improvements to HotSpot methods in DC tracking
* Switch RK4 for AM4.
* Compare reconstruction results via `compareBanks.groovy`.
* Get a good understanding on how swimming works.
* Decide on how to and if to optimize swimming.
* Compare reconstruction results via `compareBanks.groovy`.
* Figure out potential improvements to clustering.

## Making a Pull Request.
Before making a pull request back to the original repository, remember to delete test files:
* `validation/advanced-tests/run-profiling-tests.sh`
After making the pull request, add them back to the repository.
