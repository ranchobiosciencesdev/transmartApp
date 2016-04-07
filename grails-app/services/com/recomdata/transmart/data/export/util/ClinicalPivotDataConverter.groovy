package com.recomdata.transmart.data.export.util

import groovy.transform.CompileStatic
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * This is helper class for converting clinical pivot data.
 */

@CompileStatic
public class ClinicalPivotDataConverter {
    private final String inputFileLoc;
    private final String study;
    private final String workingDirectory;
    private boolean multipleStudies;
    private boolean deleteFlag;
    private boolean snpDataExists;
    private Map<String, String> dataFile;
    private Set<String> dataFilePatientIdSet;
    private Set<String> dataFileConceptPathSet;

    CacheManager cm = CacheManager.getInstance();
    

    public ClinicalPivotDataConverter(boolean multipleStudies, String study, String inputFileLoc,
                                      String workingDirectory, boolean deleteFlag, boolean snpDataExists) {
        this.inputFileLoc = inputFileLoc;
        this.study = study;
        this.multipleStudies = multipleStudies;
        this.workingDirectory = workingDirectory;
        this.deleteFlag = deleteFlag;
        this.snpDataExists = snpDataExists;
    }


    private void readMatrix() throws IOException {
        File baseFile = new File(inputFileLoc);
        //create data files
        dataFile = new HashMap<String, String>();//PATIENT.ID + CONCEPT_PATH, VALUE
        dataFilePatientIdSet = new HashSet<String>();//PATIENT.ID
        dataFileConceptPathSet = new HashSet<String>();//CONCEPT_PATH

        String[] buf;
        String s;
        //read file
        FileReader fr = new FileReader(baseFile);
        BufferedReader br = new BufferedReader(fr);
        br.readLine();

        cm.addCache("cache1");
        Cache cache = cm.getCache("cache1");

        while ((s = br.readLine()) != null) {
            buf = s.split("\t");
            buf[0] = buf[0].replace("\"", "");
            buf[3] = buf[3].replace("\"", "");
            buf[4] = buf[4].replace("\"", "");
            if (snpDataExists) {

            }

            cache.put(new Element(buf[0] + buf[3], buf[4]));

//            dataFile.put(buf[0] + buf[3], buf[4]);
            dataFileConceptPathSet.add(buf[3]);
            dataFilePatientIdSet.add(buf[0]);

        }
        br.close();
        fr.close();
    }

    private void readMatrixSnp() throws IOException {
        File baseFile = new File(inputFileLoc);
        //create data files
        dataFile = new HashMap<String, String>();//PATIENT.ID + CONCEPT_PATH, VALUE
        dataFilePatientIdSet = new HashSet<String>();//PATIENT.ID
        dataFileConceptPathSet = new HashSet<String>();//CONCEPT_PATH
        int indexSpn = -1;

        String[] buf;
        String s;
        //read file
        FileReader fr = new FileReader(baseFile);
        BufferedReader br = new BufferedReader(fr);
        s = br.readLine();
        buf = s.split("\t");
        for (int index = 0; index < buf.length; index++)
            if (buf[index].equals("SNP PED File"))
                indexSpn = index;
        while ((s = br.readLine()) != null) {
            buf = s.split("\t");
            buf[0] = buf[0].replace("\"", "");
            buf[3] = buf[3].replace("\"", "");
            buf[4] = buf[4].replace("\"", "");

            if (snpDataExists && indexSpn > 0) {
                buf[indexSpn] = buf[indexSpn].replace("\"", "");
                dataFilePatientIdSet.add(buf[0] + "\t" + buf[indexSpn]);
            }
            dataFile.put((buf[0] + buf[3]), buf[4]);
            dataFileConceptPathSet.add(buf[3]);
        }
        br.close();
        fr.close();
    }

    public void convert() throws Exception {
        if (snpDataExists)
            readMatrixSnp();
        else
            readMatrix();
        String fileName;
        fileName = "clinical_i2b2trans.txt";
        if (multipleStudies) {
            fileName = study + " _" + fileName;
        }
        fileName = workingDirectory + "//" + fileName;
        if (snpDataExists)
            pivotSpn(fileName);
        else
            pivot(fileName);

        if (deleteFlag) {
            File baseFile = new File(inputFileLoc);
            baseFile.delete();
        }

    }

    private void pivot(String outputFileName) throws IOException {

//        int rowCount = dataFilePatientIdSet.size() + 1;
//        int columnCount = dataFileConceptPathSet.size() + 1;
        String[] dataFilePatientIdArray = dataFilePatientIdSet.toArray(new String[dataFilePatientIdSet.size()]);
        String[] dataFileConceptPathArray = dataFileConceptPathSet.toArray(new String[dataFileConceptPathSet.size()]);
        Arrays.sort(dataFilePatientIdArray);
//        String[][] matrix = new String[rowCount][columnCount];
//        matrix[0][0] = "PATIENT ID";


        File f = new File(outputFileName);
        BufferedWriter writer;
        f.createNewFile();
        writer = new BufferedWriter(new FileWriter(outputFileName));

//        for (int indexRow = 0; indexRow < rowCount; indexRow++) {
//            for (int indexColumn = 0; indexColumn < columnCount; indexColumn++) {
//                if (indexRow == 0 && indexColumn != 0) {
//                    matrix[0][indexColumn] = dataFileConceptPathArray[indexColumn - 1];
//                } else if (indexColumn == 0 && indexRow != 0) {
//                    matrix[indexRow][0] = dataFilePatientIdArray[indexRow - 1];
//                } else if (indexColumn != 0) {
//                    matrix[indexRow][indexColumn] = dataFile.get(
//                            dataFilePatientIdArray[indexRow - 1] + dataFileConceptPathArray[indexColumn - 1]);
//                    if (matrix[indexRow][indexColumn] == null)
//                        matrix[indexRow][indexColumn] = "NA";
//                }
//            }
//        }

        Cache cache = cm.getCache("cache1")

        writer.write("PATIENT ID");
        writer.write("\t");

        int indexColumn = 0;

        for (String temp : dataFileConceptPathArray) {
            writer.write(temp);
            if (indexColumn != dataFileConceptPathArray.length - 1) {
                writer.write("\t");
                indexColumn++;
            }

        }
        writer.write("\n")



        for (String dataFilePatientId : dataFilePatientIdArray) {
            writer.write(dataFilePatientId);
            writer.write("\t");
            indexColumn = 0;

            for (String dataFileConceptPath : dataFileConceptPathArray) {
                String value

                if (cache.isKeyInCache(dataFilePatientId + dataFileConceptPath)) {
                    value = cache.get(dataFilePatientId + dataFileConceptPath).getObjectValue().toString();
                } else {
                    value = "NA";
                }

                writer.write(value);
                indexColumn++;
                if (indexColumn != dataFileConceptPathArray.length - 1)
                    writer.write("\t");
            }
            writer.write("\n")
            writer.flush();

        }

        writer.close();


//        writeFile(matrix, outputFileName);
    }

    private void pivotSpn(String outputFileName) throws IOException {

        int rowCount = dataFilePatientIdSet.size() + 1;
        int columnCount = dataFileConceptPathSet.size() + 2;
        String[] dataFilePatientIdArray = dataFilePatientIdSet.toArray(new String[dataFilePatientIdSet.size()]);
        String[] dataFileConceptPathArray = dataFileConceptPathSet.toArray(new String[dataFileConceptPathSet.size()]);
        Arrays.sort(dataFilePatientIdArray);
        String[][] matrix = new String[rowCount][columnCount];
        matrix[0][0] = "PATIENT ID";

        for (int indexRow = 0; indexRow < rowCount; indexRow++) {
            for (int indexColumn = 0; indexColumn < columnCount - 1; indexColumn++) {
                if (indexRow == 0 && indexColumn != 0) {
                    matrix[0][indexColumn] = dataFileConceptPathArray[indexColumn - 1];
                } else if (indexColumn == 0 && indexRow != 0) {
                    String[] buf = dataFilePatientIdArray[indexRow - 1].split("\t");
                    matrix[indexRow][0] = buf[0];
                    matrix[indexRow][columnCount - 1] = buf[1];
                } else if (indexColumn != 0) {
                    matrix[indexRow][indexColumn] = dataFile.get(
                            dataFilePatientIdArray[indexRow - 1] + dataFileConceptPathArray[indexColumn - 1]);
                    if (matrix[indexRow][indexColumn] == null)
                        matrix[indexRow][indexColumn] = "NA";
                }
            }
        }
        writeFile(matrix, outputFileName);
    }



    public void writeFile(String[][] matrix, String outputFileName) throws IOException {
        File f = new File(outputFileName);
        BufferedWriter writer;
        f.createNewFile();
        writer = new BufferedWriter(new FileWriter(outputFileName));

        for (int indexRow = 0; indexRow < matrix.length; indexRow++) {
            for (int indexColumn = 0; indexColumn < matrix[0].length; indexColumn++) {
                writer.write(matrix[indexRow][indexColumn]);
                if (indexColumn != matrix[0].length - 1)
                    writer.write("\t");
            }
            writer.write(System.getProperty("line.separator"));
            writer.flush();
        }
        writer.close();
    }
}
