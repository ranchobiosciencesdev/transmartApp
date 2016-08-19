package com.recomdata.transmart.util
import com.recomdata.transmart.domain.i2b2.ExtData
import org.apache.commons.net.ftp.FTPClient
import org.transmartproject.db.ontology.I2b2
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
        for (String url : URLs) {
            URL aURL = new URL(url)

            String server = aURL.getHost()
            int port = 21

            String username = "anonymous"
            String password = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName()

            FTPClient ftp = new FTPClient()
            FileOutputStream fos = null

            boolean successLogin = false
            int connectingAttempts = 0

//            println("######################################################################################################3")
//            println(aURL.getFile())
//            println(aURL.getHost())
//            println(aURL.getPort())
//            println(aURL.getFile())
//            println(aURL.getPath())
//            println(url.substring(url.lastIndexOf('/') + 1))

            while (true) {
                ftp.connect(server, port)
                successLogin = ftp.login(username, password)

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
                String filename = aURL.getFile()
//                fos = new FileOutputStream(dirToDownloadTo + "/" + filename)
                fos = new FileOutputStream(dirToDownloadTo + "/" + url.substring(url.lastIndexOf('/') + 1))



//            boolean downloadingSuccess = client.retrieveFile(filename, fos)
                boolean downloadingSuccess = ftp.retrieveFile(aURL.getPath(), fos)

                if (downloadingSuccess) {
                    println("File was successful downloaded from FTP server!")
                } else {
                    throw new Exception("Error of file downloading!")
                }
                fos.flush()
                fos.close()
                ftp.disconnect()
            }
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