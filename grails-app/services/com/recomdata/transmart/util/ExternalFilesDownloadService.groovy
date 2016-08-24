package com.recomdata.transmart.util
import com.recomdata.transmart.domain.i2b2.ExtData
import org.apache.commons.net.ftp.FTPClient
/**
 * Created by transmart on 8/8/16.
 */
class ExternalFilesDownloadService {
    def downloadFileFromFTPServer(ExtData extData, String dirToDownloadTo) {
        String url = extData.link;
        URL aURL = new URL(url)
        String server = aURL.getHost()

        FTPClient ftp = new FTPClient()
        FileOutputStream fos = null

        int port = 21
        int connectingAttempts
        boolean successLogin

        if (extData.getLogin() == null || extData.getPassword() == null
                || extData.getLogin().equals("") || extData.getPassword().equals("")) {

            String username = "anonymous"
            String password = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName()

            successLogin = false
            connectingAttempts = 0

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
        } else {
            String username = extData.getLogin()
            String password = extData.getPassword()

            successLogin = false
            connectingAttempts = 0

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
        }

        if (successLogin) {
            String filename = aURL.getFile()
            fos = new FileOutputStream(dirToDownloadTo + "/" + url.substring(url.lastIndexOf('/') + 1))
//            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            ftp.enterLocalPassiveMode()
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