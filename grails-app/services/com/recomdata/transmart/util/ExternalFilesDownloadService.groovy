package com.recomdata.transmart.util

import com.recomdata.transmart.domain.i2b2.ExtData
import org.apache.commons.net.ftp.FTP
import org.transmartproject.db.ontology.I2b2

import org.apache.commons.net.ftp.FTPClient;

/**
 * Created by transmart on 8/8/16.
 */
class ExternalFilesDownloadService {
    def i2b2ExportHelperService

    String studyName
    def fileLinks
    def fileNames
    def pathToTheCurrentStudy
    def fileDownloadService


    List<String> names = new ArrayList<>()
    List<String> URLs = new ArrayList<>()

    def downloadFileFromFTPServer(List<String> URLs, String dirToDownloadTo) {
//        String server = "192.168.20.223"
//        int port = 21
//        String user = "root"
//        String pass = "root"


        String server = "ftp.example.com"
        int port = 21
        String user = "login"
        String pass = "password"


        FTPClient client = new FTPClient()
        FileOutputStream fos = null

        boolean successLogin = false
        int connectingAttempts = 0

        while (true) {
            client.connect(server, port)
            successLogin = client.login(user, pass)

            if (successLogin) {
                println("FTP server - successful login!")
                break
            } else {
                println("FTP server - login denied")
                connectingAttempts++
                if (connectingAttempts > 3) break
            }

        }


        if (successLogin) {
            String filename = "neoGAA_Mapping_File.txt"
            fos = new FileOutputStream(dirToDownloadTo + "/" + filename)


//            boolean downloadingSuccess = client.retrieveFile(filename, fos)
            boolean downloadingSuccess = client.retrieveFile("/Sergey_Aleshchenko/neoGAA_Mapping_File.txt", fos)

            if (downloadingSuccess) println("File was successful downloaded from FTP server!")
            fos.flush()
            fos.close()
            client.disconnect()
        }

    }


    public void getFileNamesAndURLsForDownloading(def jobDataMap) {
        String[] subset = new String[2]

        subset[0] = jobDataMap.result_instance_ids['subset1']
        subset[1] = jobDataMap.result_instance_ids['subset2']

        studyName = i2b2ExportHelperService.findStudyAccessions(subset)
        studyName = studyName.substring(1, studyName.length() - 1)

        pathToTheCurrentStudy = I2b2.executeQuery('SELECT IB.fullName FROM I2b2 IB WHERE IB.level = 1 AND IB.fullName LIKE (\'%\' || :studyName || \'%\') ', [studyName: studyName])

        fileLinks = ExtData.executeQuery('SELECT ED.link FROM ExtData ED WHERE ED.study = :study', [study: pathToTheCurrentStudy])
        fileNames = ExtData.executeQuery('SELECT ED.name FROM ExtData ED WHERE ED.study = :study', [study: pathToTheCurrentStudy])

        for (int i = 0; i < fileNames.size(); i++) {
            names.add(fileNames.get(i) as String)

            URLs.add(fileLinks.get(i) as String)
        }

    }


    public void downloadFiles(String dirToDownloadTo, def jobDataMap) {
        getFileNamesAndURLsForDownloading(jobDataMap)

        List<String> httpURLs = new ArrayList<>()
        List<String> ftpURLs = new ArrayList<>()

        for (String URL : URLs) {
            if (URL.substring(0, 4).equals("http") || URL.substring(0, 5).equals("https")) {
                httpURLs.add(URL)
            } else if (URL.substring(0, 3).equals("ftp")) {
                ftpURLs.add(URL)
            }
        }



        fileDownloadService.getFiles(httpURLs, dirToDownloadTo)

        downloadFileFromFTPServer(ftpURLs, dirToDownloadTo)

    }

}