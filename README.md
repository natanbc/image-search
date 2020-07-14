# Image Search
### Subject
- Digital Image Processing (SCC0251).
- First semester of 2020.
- Moacir A. Ponti
### Authors
- Matheus Branco Borella (11218897)
- Natan Bernardi Cerdeira (11218984)

## Goal
The aim of this project is to index images in a folder and add options for 
searching those images by text or other factors in them, or searching for 
similar images given a sample.

## Implemented Features and Capabilities
### Image Description
The following image descriptors have been implemented into the program, followed
by the types of their respective descriptions (from here on out called `tags`),
categorized by which domain they operate in:
- Frequency domain
    - Dominant frequency band (2-Dimensional Vector)
- Magnitude domain
    - Histogram (255-Dimensional Vector)
    - Haralick descriptors
        - Contrast (Double)
        - Correlation (Double)
        - Energy (Double)
        - Entropy (Double)
        - Homogeneity (Double)
        - Maximum probability (Double)
- OCR
    - Text in the image (String, provided by the 
    [Tesseract](https://github.com/tesseract-ocr/) project)

From now on, image descriptors will be referred to as `taggers`. 

### Storage and Query Operations
Firstly, it is important to note that in order to facilitate searching for image
features and sorting them into categories, this project makes use of an 
`sqlite3` database file, always stored in the current working directory under
the path `./index.db`.

Therefore, any image that is to be operated on or taken into account when 
performing queries related to other images must have first been added to the
database file.

When an image gets added to the database, by default, the program will perform
every tagging operation supported and save their results to the database. It is
highly recommended keeping this behavior enabled, as during queries related to
a given tag all images that don't have that tag in the database will be skipped
over. There exist commands to run the taggers after an image has already been 
added, however.  

When it comes to the management of images in the database, the following
operations are available to you:
- `add [-s|--skip-tagging] <path>` Adds the image at the given path to the
database, tagging it unless the `-s` option gets specified.
- `query [-s|--select=<selection>]...` Queries all the images in the database 
or, optionally, queries for images with specific characteristics, given by the
`-s` option, which will perform a selection on the database. Valid values of 
`-s` are in the form `'[Tag] [Operator] [Value]'`, with the following 
operators (keep in mind that `[Value]`, given as a string literal, must me
convertible to the type of the queried tag):
    - `=`  Rows of `[Tag]` whose value is equal to `[Value]`.
    - `<>` Rows of `[Tag]` whose value is not equal to `[Value]`.
    - `<`  Rows of `[Tag]` whose value is less than `[Value]`.
    - `>`  Rows of `[Tag]` whose value is greater than `[Value]`.
    - `<=`  Rows of `[Tag]` whose value is less than or equal to `[Value]`.
    - `>=`  Rows of `[Tag]` whose value is greater than or equal to `[Value]`.
    - `~`  Rows of `[Tag]` whose value is like `[Value]`.
- `get <UUID> <TAG>` Gets and outputs the value, as a `String`, of the specified 
tag for the image whose ID is equal to the given UUID.
- `pass [-s|--select=<selection>]... [-t|--tagger=<tagger>]...` Will run every
tagger specified with the `-t` option (Or all taggers, if none got specified),
on every image that matches the given selection parameters, given by `-s` (Or 
all images, if no selection got specified).
- `distances [-n|--number=<number>] <UUID> <TAG>` Will get the distances between
the given tag in the image whose ID is equal as the given UUID and every other 
image in the database, showing all images sorted by their distance to the current
image, in increasing order. In case the `-n` option gets specified, only the given
number of the closest images will be displayed.

### Multithreading
During tagging and distance calculation, this program is fully capable of using
every hardware thread in the CPU of the host computer. This is achieved by having 
all tagging and distance calculation operation, which are in this program 
guaranteed to be thread-safe, be submitted as a task to run on a 
`ThreadPoolExecutor`.

## Installation, Building and Usage
In order to run or build this program you must have 
[Java 14](https://jdk.java.net/14) or greater installed. 

### A Foreword on Performance
For best performance results,
a real CPU with more than 4 threads is recommended, but not required. Just 
keep in mind that, if you choose to run this on a lower-end or shared vCPU, the 
algorithms implemented in this program are not exactly cheap to run, especially
with a high volume of high resolution images.

If this is your use case, expect to be waiting a few minutes every time you
run either `pass` or `distance`.

### Installation
You can get the pre-built jar file for this project under its 
[Releases](https://github.com/natanbc/image-search/releases) section. If you 
also wish to have support for the Tesseract-based tagger, you will also need to
have its `.traineddata` files in your current working directory or in the
directory pointed to by the `TESSDATA_PREFIX` environment variable.

### Building
If you wish to build this project you can do so by simply running the Gradle
`:run` task or by building the jar file using `:shadowJar`, in which case the
built jar file will be found in the `build/libs` directory.

Your command lines should look close to this:
- `./gradlew run --args='<program_arguments>'`
- `./gradlew shadowJar`

## Usage Examples
What follows are some examples of what we think will be common operations and
use cases for this program.

### Adding Images, with and without Tagging
Let's say you want to add the example image at `images/lem.png` to the database,
also running all taggers on the image.

```
mbr% java -jar image-search.jar add images/lem.jpg

Image ID:   67e8e1a7-1cd1-4b01-b915-b37774add4ba
Image path: images\lem.jpg
Tags:
    histogram:           [0, 0, 0, 0, 0, 1, 23, 200, 572, 815, 708, 660, (...)
    haralickContrast:    4.808358618819185
    tesseract:           ieee (...)
    haralickHomogeneity: 0.6580609598703262
    haralickMaxProb:     0.04795484209991016
    frequencyBand:       [46, 47]
    haralickCorrelation: -90.27186580487098
    haralickEntropy:     7.805771618320854
    haralickEnergy:      0.01007199022418765
```

Likewise, if you wanted to add the same image to the database, without running
any taggers.
```
mbr% java -jar image-search.jar add -s images/lem.jpg

Image ID:   a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe
Image path: images\lem.jpg
Tags:
    histogram:           NULL
    haralickContrast:    NULL
    tesseract:           NULL
    haralickHomogeneity: NULL
    haralickMaxProb:     NULL
    frequencyBand:       NULL
    haralickCorrelation: NULL
    haralickEntropy:     NULL
    haralickEnergy:      NULL
```

Notice that when you do that, all the tags associated with the image will 
have a value of `NULL`. In order to actually get any useful information out
of it you will have to tag it later.

### Tagging an Image already in the Database
Now, let's suppose you want to tag an image that is already registered in the
database, but for whatever reason didn't get tagged. Let's also say you only
want the `frequencyBand` tagger to be run on it. The image that you want to tag
is the same as the one which was added in the end of the previous section: 
`a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe`.

```
mbr% java -jar image-search.jar pass -s id=a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe -t frequencyBand

Image ID:   a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe
Image path: images\lem.jpg
Tags:
    histogram:           NULL
    haralickContrast:    NULL
    tesseract:           NULL
    haralickHomogeneity: NULL
    haralickMaxProb:     NULL
    frequencyBand:       [46, 47]
    haralickCorrelation: NULL
    haralickEntropy:     NULL
    haralickEnergy:      NULL

```
Note that the value of `frequencyBand` was changed to the vector `[46, 47]`. 
Now, let's run all the remaining taggers on the image.

```
mbr% java -jar image-search.jar pass -s id=a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe
Warning: Invalid resolution 0 dpi. Using 70 instead.

Image ID:   a5c4f8d4-91c2-41eb-ad89-d84c277a7bbe
Image path: images\lem.jpg
Tags:
    histogram:           [0, 0, 0, 0, 0, 1, 23, 200, 572, 815, 708, 660, (...)
    haralickContrast:    4.808358618819185
    tesseract:           ieee (...)
    haralickHomogeneity: 0.6580609598703262
    haralickMaxProb:     0.04795484209991016
    frequencyBand:       [46, 47]
    haralickCorrelation: -90.27186580487098
    haralickEntropy:     7.805771618320854
    haralickEnergy:      0.01007199022418765
mbr%
```
Notice how all the values for this image match the values of the first image.
This makes as both images entries have the same source file: `images/lem.png`.

### Querying for Images
Querying images with `query` has the exact same syntax as `pass`, except it does
not modify the database in any way. Querying images should therefore need no
example specific to it. 
 
### Finding the Closest Images by a Given Measure
Let's say you have added all the example images under `images/` and, given a 
reference image, wanted to find the image which has the closest value for the
`frequencyBand` tag to that of your reference, together with ranking all the
other images in terms of how close they are to your reference in that measure.

The database looks like:
```
mbr% java -jar image-search.jar query

Image ID:   ee33b707-c6e9-478d-989e-e1e656325992
Image path: .\images\manga.jpg
Tags:
    histogram:           [2674, 1614, 806, 586, 565, 482, 540, 414, 590, 389, 398, 25 (...)
    haralickContrast:    144.57893799002122
    tesseract:           Newitt (...)
    haralickHomogeneity: 0.4268124369261911
    haralickMaxProb:     0.12342599192207175
    frequencyBand:       [48, 49]
    haralickCorrelation: -6.792911860262082
    haralickEntropy:     9.08099005851536
    haralickEnergy:      0.019925592332576382

Image ID:   d4f89843-bd62-4f78-90a2-583bc64aaa4a
Image path: .\images\lem.jpg
Tags:
    histogram:           [0, 0, 0, 0, 0, 1, 23, 200, 572, 815, 708, 660, 618, 711, 87 (...)
    haralickContrast:    4.808358618819185
    tesseract:           ieee (...)
    haralickHomogeneity: 0.6580609598703262
    haralickMaxProb:     0.04795484209991016
    frequencyBand:       [46, 47]
    haralickCorrelation: -90.27186580487098
    haralickEntropy:     7.805771618320854
    haralickEnergy:      0.01007199022418765

Image ID:   b2ad5d66-c9d0-4d39-a5ff-12540b6f72fd
Image path: .\images\white.png
Tags:
    histogram:           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (...)
    haralickContrast:    0.0
    tesseract:
    haralickHomogeneity: 1.0
    haralickMaxProb:     1.0
    frequencyBand:       [40, 41]
    haralickCorrelation: NULL
    haralickEntropy:     0.0
    haralickEnergy:      1.0

Image ID:   8c331c76-d44d-4cf6-a6a9-5ab9fbacc753
Image path: .\images\squid.png
Tags:
    histogram:           [24073, 673, 307, 207, 162, 115, 124, 99, 90, 80, 79, 153, 1 (...)
    haralickContrast:    1095.367645509858
    tesseract:           me 3 oan (...)
    haralickHomogeneity: 0.10855593247089396
    haralickMaxProb:     0.03912687889564927
    frequencyBand:       [60, 61]
    haralickCorrelation: -77.260847518715
    haralickEntropy:     6.589185857093998
    haralickEnergy:      0.013373194279597135

Image ID:   630fb0f9-8e7d-4141-b92c-dde8a21e6498
Image path: .\images\anime.jpg
Tags:
    histogram:           [60, 2147, 5501, 2370, 3954, 8073, 8208, 8805, 11049, 8466,  (...)
    haralickContrast:    17.460388378744558
    tesseract:           - ~~ . (...)
    haralickHomogeneity: 0.7902778357359612
    haralickMaxProb:     0.1556043956043956
    frequencyBand:       [44, 45]
    haralickCorrelation: -15.574287046578965
    haralickEntropy:     6.225912959443762
    haralickEnergy:      0.057620114966766396

Image ID:   e21c27f4-6003-430b-a8bd-5a74af9d44b1
Image path: .\images\final_project.png
Tags:
    histogram:           [1953, 851, 792, 369, 212, 229, 712, 149, 237, 156, 146, 75, (...)
    haralickContrast:    188.2460548901618
    tesseract:           scc0251/5830 Prof. Moacir A. Ponti (...)
    haralickHomogeneity: 0.90998956783616
    haralickMaxProb:     0.9029624486489614
    frequencyBand:       [52, 53]
    haralickCorrelation: -1861.3199533825482
    haralickEntropy:     1.0767017186361705
    haralickEnergy:      0.8155423542269085

Image ID:   d6e4a538-ec92-480b-aca2-86a5bc1dd18d
Image path: .\images\dog.jpg
Tags:
    histogram:           [3724, 4702, 15545, 21283, 13179, 9043, 6387, 5112, 4623, 41 (...)
    haralickContrast:    17.386505967151233
    tesseract:           4) (...)
    haralickHomogeneity: 0.789359696838474
    haralickMaxProb:     0.17435809908928188
    frequencyBand:       [76, 77]
    haralickCorrelation: -8.636184842726642
    haralickEntropy:     6.369875758617658
    haralickEnergy:      0.05755028922255039

Image ID:   ef440261-cd10-48f1-8a97-e57864b4c2d1
Image path: .\images\yoi.png
Tags:
    histogram:           [0, 0, 0, 114, 592, 561, 689, 777, 779, 845, 1474, 1746, 267 (...)
    haralickContrast:    14.73957356160102
    tesseract:           y Oy) (...)
    haralickHomogeneity: 0.8054340044062948
    haralickMaxProb:     0.07460066187200333
    frequencyBand:       [58, 59]
    haralickCorrelation: -27.35595842679505
    haralickEntropy:     7.155794990346729
    haralickEnergy:      0.018774773706988473
mbr%
```

Taking `images/dog.jpg` as our reference, with ID `d6e4a538-ec92-480b-aca2-86a5bc1dd18d`,
we can rank the images by doing:
```
mbr% java -jar image-search.jar distances d6e4a538-ec92-480b-aca2-86a5bc1dd18d frequencyBand
[1/7] With a distance of 22.627417
Image ID:   8c331c76-d44d-4cf6-a6a9-5ab9fbacc753
Image path: .\images\squid.png
Tags:
    histogram:           [24073, 673, 307, 207, 162, 115, 124, 99, 90, 80, 79, 153, 1 (...)
    haralickContrast:    1095.367645509858
    tesseract:           me 3 oan (...)
    haralickHomogeneity: 0.10855593247089396
    haralickMaxProb:     0.03912687889564927
    frequencyBand:       [60, 61]
    haralickCorrelation: -77.260847518715
    haralickEntropy:     6.589185857093998
    haralickEnergy:      0.013373194279597135
[2/7] With a distance of 25.455844
Image ID:   ef440261-cd10-48f1-8a97-e57864b4c2d1
Image path: .\images\yoi.png
Tags:
    histogram:           [0, 0, 0, 114, 592, 561, 689, 777, 779, 845, 1474, 1746, 267 (...)
    haralickContrast:    14.73957356160102
    tesseract:           y Oy) (...)
    haralickHomogeneity: 0.8054340044062948
    haralickMaxProb:     0.07460066187200333
    frequencyBand:       [58, 59]
    haralickCorrelation: -27.35595842679505
    haralickEntropy:     7.155794990346729
    haralickEnergy:      0.018774773706988473
[3/7] With a distance of 33.941125
Image ID:   e21c27f4-6003-430b-a8bd-5a74af9d44b1
Image path: .\images\final_project.png
Tags:
    histogram:           [1953, 851, 792, 369, 212, 229, 712, 149, 237, 156, 146, 75, (...)
    haralickContrast:    188.2460548901618
    tesseract:           scc0251/5830 Prof. Moacir A. Ponti (...)
    haralickHomogeneity: 0.90998956783616
    haralickMaxProb:     0.9029624486489614
    frequencyBand:       [52, 53]
    haralickCorrelation: -1861.3199533825482
    haralickEntropy:     1.0767017186361705
    haralickEnergy:      0.8155423542269085
[4/7] With a distance of 39.597980
Image ID:   ee33b707-c6e9-478d-989e-e1e656325992
Image path: .\images\manga.jpg
Tags:
    histogram:           [2674, 1614, 806, 586, 565, 482, 540, 414, 590, 389, 398, 25 (...)
    haralickContrast:    144.57893799002122
    tesseract:           Newitt (...)
    haralickHomogeneity: 0.4268124369261911
    haralickMaxProb:     0.12342599192207175
    frequencyBand:       [48, 49]
    haralickCorrelation: -6.792911860262082
    haralickEntropy:     9.08099005851536
    haralickEnergy:      0.019925592332576382
[5/7] With a distance of 42.426407
Image ID:   d4f89843-bd62-4f78-90a2-583bc64aaa4a
Image path: .\images\lem.jpg
Tags:
    histogram:           [0, 0, 0, 0, 0, 1, 23, 200, 572, 815, 708, 660, 618, 711, 87 (...)
    haralickContrast:    4.808358618819185
    tesseract:           ieee (...)
    haralickHomogeneity: 0.6580609598703262
    haralickMaxProb:     0.04795484209991016
    frequencyBand:       [46, 47]
    haralickCorrelation: -90.27186580487098
    haralickEntropy:     7.805771618320854
    haralickEnergy:      0.01007199022418765
[6/7] With a distance of 45.254834
Image ID:   630fb0f9-8e7d-4141-b92c-dde8a21e6498
Image path: .\images\anime.jpg
Tags:
    histogram:           [60, 2147, 5501, 2370, 3954, 8073, 8208, 8805, 11049, 8466,  (...)
    haralickContrast:    17.460388378744558
    tesseract:           - ~~ . (...)
    haralickHomogeneity: 0.7902778357359612
    haralickMaxProb:     0.1556043956043956
    frequencyBand:       [44, 45]
    haralickCorrelation: -15.574287046578965
    haralickEntropy:     6.225912959443762
    haralickEnergy:      0.057620114966766396
[7/7] With a distance of 50.911688
Image ID:   b2ad5d66-c9d0-4d39-a5ff-12540b6f72fd
Image path: .\images\white.png
Tags:
    histogram:           [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (...)
    haralickContrast:    0.0
    tesseract:
    haralickHomogeneity: 1.0
    haralickMaxProb:     1.0
    frequencyBand:       [40, 41]
    haralickCorrelation: NULL
    haralickEntropy:     0.0
    haralickEnergy:      1.0
mbr%
```
With this, we have found that the closest image to `images/dog.jpg`, in terms of
its dominant frequency band, is `images/squid.png`.
