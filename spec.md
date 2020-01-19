# Project Configurations

The flow of the program is as follows:

On PC, the image gets encrypted, shuffled and encoded.

On Android, the image gets decoded, de-shuffled and decrypted.

*Table of contents*:
- [Position Detectors](#Position-detectors-type/quantity)
- [Encoding and Decoding](#Encoding-And-Decoding)
- [Input image size, as function of other parameters](#input-image-size-as-function-of-other-parameters)
- [Checksum](#Checksum)
- [IV](#IV)
- [Encryption and Decryption](#Encryption-and-Decryption)
- [Shuffle](#Shuffle)

### Position Detectors
Square QR type position detectors – in 3 different corners of the encoded image (top left, top right, bottom left).
Each dimension of each square is 7 modules.
Position detectors are used in the aforementioned manner in order to identify the image borders and to resolve image rotation. 

### Encoding and Decoding 
Module - The basic data unit that consists square block of pixels.
Each module is assigned with certain color out of #COLOR_LEVELS_USED available colors -
encoded data corresponding to the assigned color.


Encoded elements are:
1. IV+checksum: Since a single bit flip in the encoded IV will result in completely different decoded image, we encode the IV twice.
The IV is encoded at the beginning & at the end of available encoding modules.
Furthermore, a checksum of the IV is also added to try and recover in case of errors.
2. Image data (including image dimensions)

* Supporting 2-64 color levels (powers of 2 only) for each module.
Encoding is done from top left available modules towards right and then down directions.
Input is byte array of (encrypted) data.
Each module is capable of encoding log2(#COLOR_LEVELS_USED) bits of the input data.
Each input byte encoded separately, LSB's first (in top leftmost modules available),
as data from two separate bytes is concatenated (newer byte data concatenated to the right of the older byte data).
Data is encoded successively (including IV, dimensions and actual image data bytes).
Decoding is process the inverse the encoding process.
Data length sent as an input to the decodeData method, enabling to decode separate data sections. 

* Position Detectors 
Square QR type position detectors – in 3 different corners of the encoded image (top left, top right, bottom left).
Each dimension of each square is 7 modules.
* Dimension encoding
Image pixel dimensions & checksum are encoded in total length 5 bytes,
as a part of the image encrypted data.
These 5 bytes are encoded twice in the beginning + end to ensure right decoding.
Encoding is done in the following order:
image width: 2 bytes -> image height: 2 bytes -> checksum: 1 byte.

### Input image size, as function of other parameters
We support any image size that is smaller or equal to the limit.
Any image that is smaller than the limit, get padded with zeros to the limit size.
Images that exceeds the limit, is not handled and an exception is thrown.

The limit is computed as the following:
Maximum input image size in bytes is computed in the following manner:
```
floor(MODULE_BITS_ENCODING_CAPACITY/8 * [MODULES_IN_ENCODED_IMAGE_DIM*MODULES_IN_ENCODED_IMAGE_DIM - 4*MODULES_IN_MARGIN *
(MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) - MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*
NUM_OF_POSITION_DETECTORS –2*(IV_AND_CHECKSUM_BIT_LENGTH+INPUT_IMAGE_DIMS_AND_CHECKSUM_BIT_LENGTH)])
```
Maximum input data size is 184185 bytes, for the following parameters:
```
MODULE_BITS_ENCODING_CAPACITY = 6 (64 color encoding levels)
MODULES_IN_ENCODED_IMAGE_DIM = 500
MODULES_IN_MARGIN = 2
MODULES_IN_POS_DET_DIM = 7
NUM_OF_POSITION_DETECTORS = 3
IV_AND_CHECKSUM_BIT_LENGTH = 13
INPUT_IMAGE_DIMS_AND_CHECKSUM_BIT_LENGTH = 5
```

### Checksum
The checksum method we use is [Sum Complement](https://en.wikipedia.org/wiki/Checksum#Sum_complement)
The information that we compute the checksum of is the IV and the encoded image height and width.
The checksum is padded right next to each information asset.
Since the information assets are duplicated in two seperate places, we take the asset that his integity is valid. 
If both checksums are invalid for a certain asset, we throw an error that this image cannot be decoded. 


### IV
In order to avoid the same image resulting in the same encoding, we use an IV. At the moment the IV is 12 bytes, but can be changed on demand.
Source code of IV generation:
```
	public static IvParameterSpec generateIv(int ivLength) {
		final byte[] iv = new byte[ivLength];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}
```

### Encryption and Decryption
The image can't be encrypted with block encryption mode, since one bit flip will result in wrong decryption. 
So we generate random bits in the length of the maximum supported image using AES and the private key. We iteratively encrypt the iv plus a counter until a random bits of the maximum length is generated.
Using those random bits, we bit-wise XOR the image bits.
Source code:
```
	public static byte[] xorPaddedImage(byte[] imageBytes, byte[] generatedXorBytes) {
		final byte[] xoredImage = new byte[MAX_ENCODED_LENGTH_BYTES];
		for (int i = 0; i < MAX_ENCODED_LENGTH_BYTES; i++) {
			xoredImage[i] = (byte) (imageBytes[i] ^ generatedXorBytes[i]);
		}
		return xoredImage;
	}
```

To decrypt the image, we first fetch the IV from the encoded image. Afterwards, using the secret key (which is hard-coded for now), the image is being xored again which reveals its real bits.

### Shuffle
In order to decrease the noise at specific area and to distribute it to minimize its effect, we shuffle the encrypted data. 
This way, a noise at a specific area, will spread to different locations in the decoded image. 
To avoid same index burnout effecting the same locations in the decoded image, the shuffle is seeded by the IV. 
The first 4 bytes of the IV is used as a seed to randomize the indexes. Source code:

```
Collections.shuffle(indexes, new Random((long) ivInt));
```


---- Current assumptions: 

Decoded image is the generated output of our encoding proccess - file is transfered manually (not captured by camera).
We support rotation of the encoded image by 0, 90, 180, 270 degrees.
We support addition of white image background.
