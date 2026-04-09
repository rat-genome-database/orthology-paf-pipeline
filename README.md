# orthology-paf-pipeline

Generates JBrowse2 synteny track files (BED, .anchors, .json) for cross-species ortholog
comparisons across all supported assemblies.

## Overview

For each configured genome assembly, the pipeline produces a BED file listing all mapped
active genes. Then for each pair of assemblies from different species, it creates:
- An `.anchors` file (MCScan format) listing orthologous gene symbol pairs
- A `.json` track config file for JBrowse2 with MCScanAnchorsAdapter and multiple display types

## Usage

```
java -jar orthology-paf-pipeline.jar <output_directory>
```

## Logic

1. **Generate BED files** — for each assembly, write a BED file of all mapped genes
2. **Generate anchors and JSON** — for each pair of assemblies from different species,
   retrieve mapped orthologs and write the synteny track files

## Supported assemblies

Configured in `Manager.main()`:
- Rat: GRCr8, mRatBN7.2, Rnor_6.0, Rnor_5.0, RGSC_v3.4
- Human: GRCh38.p14, GRCh37.p13, NCBI36
- Mouse: GRCm39, GRCm38, MGSCv37
- Dog: CanFam3.1, UU_Cfam_GSD_1.0, Dog10K_Boxer_Tasha, ROS_Cfam_1.0
- Pig: Sscrofa11.1, Sscrofa10.2
- Green monkey: Chlorocebus_sabeus1.1, Vero_WHO_p1.0
- Bonobo: Mhudiblu_PPA_v0, panpan1.1
- Naked mole-rat: HetGla_female_1.0
- Chinchilla: ChiLan1.0
- Squirrel: SpeTri2.0

## Build and run

Requires Java 17. Built with Gradle:
```
./gradlew clean assembleDist
```
