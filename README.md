
# hc-printer

## Getting started

`$ npm install hc-printer --save`

### Mostly automatic installation

`$ react-native link hc-printer`

### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNPrinterPackage;` to the imports at the top of the file
  - Add `new RNPrinterPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':hc-printer'
  	project(':hc-printer').projectDir = new File(rootProject.projectDir, 	'../node_modules/hc-printer/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':hc-printer')
  	```


## Usage
```javascript
import RNPrinter from 'hc-printer';

// TODO: What to do with the module?
RNPrinter;
```
  