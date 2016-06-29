/*
 * COMSOLDataExtractor.java
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Comparable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import com.comsol.model.*;
import com.comsol.model.util.*;

public class COMSOLDataExtractor {
  private Model model_;
  private float[] times_ = null;
  private float[][][] data_ = null;
  private String plotGroupName_ = null;
  
  /****************** PUBLIC INTERFACE ******************/
  private COMSOLDataExtractor() {
  }

  public static COMSOLDataExtractor load(File modelFile) throws java.io.IOException {
    System.out.println(modelFile.getAbsolutePath());
    COMSOLDataExtractor extractor = new COMSOLDataExtractor();
    extractor.model_ = ModelUtil.loadCopy(ModelUtil.uniquetag("Model"), modelFile.getAbsolutePath());
    
    return extractor;
  }

  public void run() {
    // final long START_TIME = System.currentTimeMillis();
    // this.model.result(plotGroup).run();
    // System.out.println("Extracting Data...");
    // System.out.flush();
    // this.model.sol("sol1").runAll();
    // this.model.batch("p1").run();
    
    String[] resultTags = this.model_.result().tags();
    /*
    System.out.println("Results:");
    for (int index = 0; index < resultTags.length; ++index) {
      System.out.println("tag: " + resultTags[index]);
    }
    */

    // The default solution selection is "all", so count the solutions
    //ResultFeature plotGroup = this.model_.result(resultTags[0]);
    ResultFeature plotGroup = this.model_.result(this.plotGroupName_);
    // FIXME: make the solution count configurable
    /*
    final int SOLUTION_COUNT = 9;
    System.out.println("# Solutions: " + SOLUTION_COUNT);
    */

    // FIXME: make the plots to extract data from configurable
    // Get the active plots in plot group plotGroup
    String[] resultFeatureTags = plotGroup.feature().tags();
    System.out.print("Extracting data from the following plots in plot group "
                    + this.plotGroupName_ + ": ");
    ArrayList<ResultFeature> plots = new ArrayList<ResultFeature>();
    for (int index = 0; index < resultFeatureTags.length; ++index) {
      if (plotGroup.feature(resultFeatureTags[index]).isActive()) {
        System.out.print(resultFeatureTags[index] + " ");
        plots.add(plotGroup.feature(resultFeatureTags[index]));
      }
    }
    System.out.println();

    final int PLOT_COUNT = plots.size();

    final ResultFeature firstPlot = plots.get(0);
    final int SOLUTION_COUNT = firstPlot.getGroups(0);
    System.out.println("Found " + SOLUTION_COUNT + " solution(s).");

    // Extract the plot data
    //  plotGroup.set("outersolnum", String.valueOf(1));
    this.times_ = firstPlot.getVertices(0, 0)[0];
    /*
    System.out.print("Times: ");
    for (int i = 0; i < this.times_.length; ++i) {
      if (i > 0) {
        System.out.print(", ");
      }
      System.out.print(this.times_[i]);
    }
    System.out.println();
    */

    final int SAMPLES_PER_SOLUTION = this.times_.length;

    // initialize the plot data array
    this.data_ = new float[SOLUTION_COUNT][PLOT_COUNT][SAMPLES_PER_SOLUTION];
    // initialize data to zeros
    for (int i = 0; i < SOLUTION_COUNT; ++i) {
      for (int j = 0; j < PLOT_COUNT; ++j) {
        for (int k = 0; k < SAMPLES_PER_SOLUTION; ++k) {
          this.data_[i][j][k] = 0;
        }
      }
    }
    /*
    final long ELAPSED_TIME = System.currentTimeMillis() - START_TIME;
    System.out.println("Elapsed Time: " + ELAPSED_TIME);
    System.out.flush();
    */

    for (int solutionIndex = 0; solutionIndex < SOLUTION_COUNT; ++solutionIndex) {
      for (int plotIndex = 0; plotIndex < PLOT_COUNT; ++plotIndex) {
        float[] plotPoints = plots.get(plotIndex).getData(0, solutionIndex, "Height");
        /*
        System.out.println("Got " + plotPoints.length + " plot points.");
        System.out.println("Samples per Solution: " + SAMPLES_PER_SOLUTION);
        System.out.println("Extracting data for plot " + plotIndex + ", solution " + solutionIndex);
        */
        System.arraycopy(plotPoints, 0,
                         this.data_[solutionIndex][plotIndex], 0,
                         SAMPLES_PER_SOLUTION);
      }
    }
  }

  public void setPlotGroupName(String plotGroupName) {
    this.plotGroupName_ = plotGroupName;
  }

  public float[][][] getData() {
    return this.data_;
  }
  
  public float[] getTimes() {
    return this.times_;
  }

  public void setS0Radius(String expression) {
    this.model_.result().dataset("cpt1").set("pointx", expression);
  }

  public void setS5Radius(String expression) {
    this.model_.result().dataset("cpt2").set("pointx", expression);
  }

  public void setS1z(String expression) {
    this.model_.result().dataset("cpt3").set("pointz", expression);
  }

  /****************** Static Helper Functions ******************/

  static byte[] NumpyHeader(int dim1, int dim2, int dim3) {
    final byte[] MAGIC_STRING = new byte[] {
      (byte) 0x93, 'N', 'U', 'M', 'P', 'Y'
    };
    final byte MAJOR_VERSION = 0x01;
    final byte MINOR_VERSION = 0x00;
    final String ARRAY_DESCRIPTOR = "{ 'descr': '<f8', 'fortran_order': False, 'shape': ("
                                  + dim1 + "," + dim2 + "," + dim3 + "),}";
    final short PADDING_LENGTH = (short) (16 - ((ARRAY_DESCRIPTOR.length() + 11) % 16));
    byte[] padding = new byte[PADDING_LENGTH];
    for (int index = 0; index < PADDING_LENGTH; ++index) {
      padding[index] = ' ';
    }
    final byte DESCRIPTOR_TERMINATOR =  '\n';
    final short DESCRIPTOR_LENGTH = (short) (ARRAY_DESCRIPTOR.length() + PADDING_LENGTH + 1);

    System.out.println("Descriptor Length: " + DESCRIPTOR_LENGTH);
    ByteBuffer header = ByteBuffer.allocate(DESCRIPTOR_LENGTH+10);
    header.order(ByteOrder.LITTLE_ENDIAN);
    try {
      header.put(MAGIC_STRING).put(MAJOR_VERSION).put(MINOR_VERSION)
            // .put((byte) (DESCRIPTOR_LENGTH & 0x00FF)).put((byte) ((DESCRIPTOR_LENGTH >> 8) & 0x00FF))
            .putShort((short) (DESCRIPTOR_LENGTH & 0xFFFF))
            .put(ARRAY_DESCRIPTOR.getBytes("US-ASCII"))
            .put(padding).put(DESCRIPTOR_TERMINATOR);
    } catch(java.io.UnsupportedEncodingException uee) {
      System.err.println(uee.getMessage());
    }
    return header.array();
  }

  /****************** MAIN ******************/
  public static void main(String[] args) {
    if ((args == null) || (args.length != 1)) {
      // System.out.println("Usage: comsolrun COMSOLDataExtractor.java <MPH Filename> <S0 Radius(mm)> <S5 Radius(mm)> <S1 z(mm)");
      System.out.println("Usage: comsolrun COMSOLDataExtractor.java <config filename>");
      return;
    }
    final String FILENAME = args[0];
    File file = new File(FILENAME);
    Properties properties = null;
    try {
      FileInputStream inputStream = new FileInputStream(file);
      properties = new Properties();
      properties.load(inputStream);
      inputStream.close();
    } catch(Exception e) {
      System.out.println("ERROR: " + e.getMessage());
      System.out.flush();
      e.printStackTrace();
      try {
          Thread.sleep(1000);                 //1000 milliseconds is one second.
      } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
      }
      return;
    }

    // Note: radii are constrained to 5mm increments
    /*
    final int S0_RADIUS = Integer.parseInt(args[1]);
    final int S5_RADIUS = Integer.parseInt(args[2]);
    final int S1_Z = Integer.parseInt(args[3]);
    */

    // #### Load the model ####
    String simDirectoryName = properties.getProperty("simDirectoryName");
    String simFileName = properties.getProperty("simFileName");
    File modelFile = new File(simDirectoryName, simFileName);
    COMSOLDataExtractor extractor = null;
    try {
      extractor = COMSOLDataExtractor.load(modelFile);
    } catch(IOException ioe) {
      System.out.println("ERROR: " + ioe.getMessage());
      System.out.flush();
      ioe.printStackTrace();
      try {
          Thread.sleep(1000);                 //1000 milliseconds is one second.
      } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
      }
      return;
    }
    extractor.setPlotGroupName(properties.getProperty("plotGroupName"));

    // Extract the plot data
    extractor.run();

    float[] times = extractor.getTimes();
    float[][][] data = extractor.getData();

    // Generate the Numpy header
    final byte[] NUMPY_HEADER = COMSOLDataExtractor.NumpyHeader(
      data.length, data[0].length, data[0][0].length);

    // Allocate a byte buffer for the numpy header and array data
    final int BUFFER_LENGTH = NUMPY_HEADER.length + data.length*data[0].length*data[0][0].length*8;
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_LENGTH);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // final String filename = "array_r_s0_" + S0_RADIUS + "_r_s5_" + S5_RADIUS + "_z_s1_" + S1_Z + ".npy";
    final String filename = simFileName.replaceAll(".mph", ".npy");
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
    } catch (java.io.FileNotFoundException fnfe) {
      System.err.println(fnfe.getMessage());
      return;
    }
    buffer.clear();
    buffer.put(NUMPY_HEADER);

    // System.out.println("r_S0: " + S0_RADIUS + " mm\tr_s5: " + S5_RADIUS + " mm\tz_s1: " + S1_Z + " mm");
    for (int solutionIndex = 0; solutionIndex < data.length; ++solutionIndex) {
      for (int plotIndex = 0; plotIndex < data[0].length; ++plotIndex) {
        for (int sampleIndex = 0; sampleIndex < data[0][0].length; ++sampleIndex) {
          buffer.putDouble(data[solutionIndex][plotIndex][sampleIndex]);
        }
      }
    }

    try {
      fos.write(buffer.array());
      fos.close();
    } catch(java.io.IOException ioe) {
      System.err.println(ioe.getMessage());
    }
  }
}
