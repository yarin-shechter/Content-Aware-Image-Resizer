# Content Aware Image Resizer
A Java application for content aware image resizing utilizing the Seam Carving algorithm. 
Different resizing options are provided within the app. In particular an image can be resized using simple techniques (nearest neighbor, bilinear interpollation) and the Seam Carving algorithm which is a content aware technique.
Below is an example of the execution in which the width of the image is reduced by ~80 pixels. Note how the algorithm chooses to remove seams in areas that include less detail.
![seamcarver](https://github.com/yarin-shechter/Content-Aware-Image-Resizer/assets/48433514/e0dcdf80-7413-4963-8510-f75804ba6a99)

