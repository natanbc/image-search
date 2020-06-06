# Image Search

Digital Image Processing Final Project

# Authors

Matheus Branco Borella  - 11218897
Natan Bernardi Cerdeira - 11218984

# Goal

The aim of this project is to index images in a folder and add options for searching
those images by text or other factors in them, or searching for similar images given
a sample (think google but images instead of text). The tasks involved are image
segmentation and image description, but might also use fourier transforms to look
for similar images.

### Input

- where the images are stored
- filters used for searching

### Output

- list of images matching the provided filters, possibly with a score for the match.


# Example

![](images/hello_world.png)

```
./find-image -d path/where/images/are --contains-text "hello world"

  <path/to/image>: matches text
```
