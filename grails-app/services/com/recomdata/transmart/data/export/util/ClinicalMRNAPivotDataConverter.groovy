package com.recomdata.transmart.data.export.util
/**
 * This is helper class for converting clinical MRNA pivot data.
 */
public class ClinicalMRNAPivotDataConverter {
    private final String inputFileLoc;
    private final String study;
    private final String workingDirectory;
    private boolean multipleStudies;
    private boolean deleteFlag;
    private boolean snpDataExists;
    private Map<String, String> dataFile;
    private Set<String> dataFilePatientIdSet;
    private Set<String> dataFileConceptPathSet;
    private Set<String> dataFileAssayIdSet;
    private List<String> dataFileAssayIdList;

    public ClinicalMRNAPivotDataConverter(boolean multipleStudies, String study, String inputFileLoc,
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
        dataFileAssayIdSet = new HashSet<String>();//ASSAY_ID
        dataFileAssayIdList = new ArrayList<String>();

        String[] buf;
        String s;
        String prevPatientId = "";
        long dataFilePatientIdSetSize = 1;
        //read file
        FileReader fr = new FileReader(baseFile);
        BufferedReader br = new BufferedReader(fr);
        br.readLine();

        while ((s = br.readLine()) != null) {
            buf = s.split("\t");
            buf[0] = buf[0].replace("\"", "");
            buf[3] = buf[3].replace("\"", "");
            buf[4] = buf[4].replace("\"", "");
            buf[6] = buf[6].replace("\"", "");

            dataFile.put(buf[0] + buf[3], buf[4]);
            dataFileConceptPathSet.add(buf[3]);
            dataFilePatientIdSet.add(buf[0]);


            if (dataFilePatientIdSet.size() > dataFilePatientIdSetSize && dataFilePatientIdSet.size() > 1) {
                String[] dataFileAssayIdArray = dataFileAssayIdSet.toArray(
                        new String[dataFileAssayIdSet.size()]);
                String tmp = Arrays.toString(dataFileAssayIdArray);
                tmp = tmp.replaceAll(", ", " | ");
                tmp = tmp.replaceAll("\\[", "");
                tmp = tmp.replaceAll("\\]", "");
                tmp = prevPatientId + "\t" + tmp;
                dataFileAssayIdList.add(tmp);
                dataFileAssayIdSet = new HashSet<String>();
                dataFilePatientIdSetSize++;
            }
            dataFileAssayIdSet.add(buf[6]);
            prevPatientId = buf[0];
        }
        if (dataFileAssayIdSet.size() > 0) {
            String[] dataFileAssayIdArray = dataFileAssayIdSet.toArray(
                    new String[dataFileAssayIdSet.size()]);
            String tmp = Arrays.toString(dataFileAssayIdArray);
            tmp = tmp.replaceAll(", ", " | ");
            tmp = tmp.replaceAll("\\[", "");
            tmp = tmp.replaceAll("\\]", "");
            tmp = prevPatientId + "\t" + tmp;
            dataFileAssayIdList.add(tmp);
            dataFileAssayIdSet = new HashSet<String>();
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
        dataFileAssayIdSet = new HashSet<String>();//ASSAY_ID
        dataFileAssayIdList = new ArrayList<String>();

        String[] buf;
        String s;
        long dataFilePatientIdSetSize = 1;
        String prevPatientId = "";
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
            buf[6] = buf[6].replace("\"", "");

            if (snpDataExists && indexSpn > 0) {
                buf[indexSpn] = buf[indexSpn].replace("\"", "");
                dataFilePatientIdSet.add(buf[0] + "\t" + buf[indexSpn]);
            }
            dataFile.put(buf[0] + buf[3], buf[4]);
            dataFileConceptPathSet.add(buf[3]);

            if (dataFilePatientIdSet.size() > dataFilePatientIdSetSize && dataFilePatientIdSet.size() > 1) {
                String[] dataFileAssayIdArray = dataFileAssayIdSet.toArray(
                        new String[dataFileAssayIdSet.size()]);
                String tmp = Arrays.toString(dataFileAssayIdArray);
                tmp = tmp.replaceAll(", ", " | ");
                tmp = tmp.replaceAll("\\[", "");
                tmp = tmp.replaceAll("\\]", "");
                tmp = prevPatientId + "\t" + tmp;
                dataFileAssayIdList.add(tmp);
                dataFileAssayIdSet = new HashSet<String>();
                dataFilePatientIdSetSize++;
                System.out.println("Lol " + dataFilePatientIdSetSize + " " + dataFilePatientIdSet.size());
            }
            dataFileAssayIdSet.add(buf[6]);
            prevPatientId = buf[0] + "\t" + buf[indexSpn];
        }
        if (dataFileAssayIdSet.size() > 0) {
            String[] dataFileAssayIdArray = dataFileAssayIdSet.toArray(
                    new String[dataFileAssayIdSet.size()]);
            String tmp = Arrays.toString(dataFileAssayIdArray);
            tmp = tmp.replaceAll(", ", " | ");
            tmp = tmp.replaceAll("\\[", "");
            tmp = tmp.replaceAll("\\]", "");
            tmp = prevPatientId + "\t" + tmp;
            dataFileAssayIdList.add(tmp);
            dataFileAssayIdSet = new HashSet<String>();
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

        int rowCount = dataFilePatientIdSet.size() + 1;
        int columnCount = dataFileConceptPathSet.size() + 2;
        String[] dataFileConceptPathArray = dataFileConceptPathSet.toArray(new String[dataFileConceptPathSet.size()]);
        String[] dataFileAssayIdArray = dataFileAssayIdList.toArray(new String[dataFileAssayIdList.size()]);

        Arrays.sort(dataFileAssayIdArray);

        String[][] matrix = new String[rowCount][columnCount];
        matrix[0][0] = "PATIENT.ID";
        matrix[0][columnCount - 1] = "ASSAY.ID";

        for (int indexRow = 0; indexRow < rowCount; indexRow++) {
            for (int indexColumn = 0; indexColumn < columnCount - 1; indexColumn++) {
                if (indexRow == 0 && indexColumn != 0) {
                    matrix[0][indexColumn] = dataFileConceptPathArray[indexColumn - 1];
                } else if (indexColumn == 0 && indexRow != 0) {
                    String[] buf = dataFileAssayIdArray[indexRow - 1].split("\t");
                    matrix[indexRow][0] = buf[0];
                    matrix[indexRow][columnCount - 1] = buf[1];
                } else if (indexColumn != 0) {
                    String[] buf = dataFileAssayIdArray[indexRow - 1].split("\t");
                    matrix[indexRow][indexColumn] = dataFile.get(
                            buf[0] + dataFileConceptPathArray[indexColumn - 1]);
                    if (matrix[indexRow][indexColumn] == null)
                        matrix[indexRow][indexColumn] = "NA";
                }
            }
        }
        writeFile(matrix, outputFileName);
    }

    private void pivotSpn(String outputFileName) throws IOException {

        int rowCount = dataFilePatientIdSet.size() + 1;
        int columnCount = dataFileConceptPathSet.size() + 3;
        String[] dataFileConceptPathArray = dataFileConceptPathSet.toArray(new String[dataFileConceptPathSet.size()]);
        String[] dataFileAssayIdArray = dataFileAssayIdList.toArray(new String[dataFileAssayIdList.size()]);

        Arrays.sort(dataFileAssayIdArray);

        String[][] matrix = new String[rowCount][columnCount];
        matrix[0][0] = "PATIENT.ID";
        matrix[0][columnCount - 2] = "ASSAY.ID";
        matrix[0][columnCount - 1] = "SNP.PED.File";
        for (int indexRow = 0; indexRow < rowCount; indexRow++) {
            for (int indexColumn = 0; indexColumn < columnCount - 1; indexColumn++) {
                if (indexRow == 0 && indexColumn != 0) {
                    matrix[0][indexColumn] = dataFileConceptPathArray[indexColumn - 1];
                } else if (indexColumn == 0 && indexRow != 0) {
                    String[] buf = dataFileAssayIdArray[indexRow - 1].split("\t");
                    matrix[indexRow][0] = buf[0];
                    matrix[indexRow][columnCount - 1] = buf[1];
                    matrix[indexRow][columnCount - 2] = buf[2];
                } else if (indexColumn != 0) {
                    String[] buf = dataFileAssayIdArray[indexRow - 1].split("\t");
                    matrix[indexRow][indexColumn] = dataFile.get(
                            buf[0] + "\t" + buf[1] + dataFileConceptPathArray[indexColumn - 1]);
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
