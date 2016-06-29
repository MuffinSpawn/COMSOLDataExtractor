COMSOLDataExtractor is a tool for extracting plot data from a COMSOL simulation (.mph file) and saving it as a NumPy data file (Python 2.7). This was a quick and dirty project, so it's not very pretty and has no installation script. That said, it's not that hard to build and install by hand since it comprises only one Java file.

Modification will be necessary if you are using Python 3, as the NumPy data format has changed. Figuring out exactly what this entails has not been high on my list of priorities.

PREREQUISITES

1) COMSOL 4.4
2) JDK 1.6
3) A BASH console

Other versions may work, but I have not tested it on any other.


BUILDING
--------
In a BASH script, one can do the following:

javac -cp $(echo COMSOL_PLUGINS_PATH/*.jar | tr ' ' ':') $@


On Windows my COMSOL_PLUGINS_PATH is (from MSYS2 BASH console)

/c/Program\ Files/COMSOL/COMSOL44/plugins.

On Ubuntu Linux my COMSOL_PLUGINS_PATH is

/usr/local/comsol44/plugins.

I'm sure it's possible to do this in a Windows batch file, but I haven't spent the time to figure it out.


INSTALLATION
------------
Copy the .class file wherever you want it (INSTALL_PATH).


RUNNING
-------
First, you need a configuration file in the Java properties file format (PROPS_FILE). An example is included in the etc/ directory within this repository.

On my Windows machine I do

/c/Program\ Files/COMSOL/COMSOL44/bin/win64/comsolbatch -nosave -mpmode owner -inputfile INSTALL_PATH/COMSOLDataExtractor.class PROPS_FILE


And on my Unbuntu Linux machine I do

/usr/local/comsol44/bin/glnxa64/comsol batch -mpmode owner -inputfile INSTALL_PATH/COMSOLDataExtractor.class PROPS_FILE
