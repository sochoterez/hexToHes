package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class Controller {


    public File file;
    public String hardwareAscii = null;
    public String hardwareVersion = null;
    public String lineCheckSum = null;
    //public String versionAscii = null;
    public String versionHex = null;
    public String versionString = null;
    public String typeDevice = null;
    public String msb = null;
    public String lsbFrom = null;
    public String lsbTo = null;
    public SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    public Date date = new Date();
    public boolean invalid = false;
    public boolean header = false;


    @FXML
    private Label labChooseFile;

    @FXML
    private Label labErrorMsg;

    @FXML
    private Label labType;

    @FXML
    private Label labVersion;

    @FXML
    private TextField txtFromAddress;

    @FXML
    private TextField txtToAddress;

    @FXML
     private Label labOutputFile;

    @FXML
    public void singleFileChooser() {

        labErrorMsg.setText("");
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HEX Files","*.hex"));

        Properties prop = new Properties();
        String fileName = ".global.config";
        try (FileInputStream fis = new FileInputStream(fileName)) {
            prop.load(fis);
        } catch (FileNotFoundException ex) {
         // FileNotFoundException catch is optional and can be collapsed
        } catch (IOException ex) {
        }
        //System.out.println(prop.getProperty("path.to.hex"));
        String pathToHex = prop.getProperty("path.to.hex");
        String pathToFile =   labChooseFile.getText();


        if ( pathToHex != null){
            pathToFile = pathToHex.substring(0, pathToHex.lastIndexOf("\\"));
        }

        File userDirectory = new File(pathToFile);

        if(!userDirectory.canRead()) {
            String userDirectoryString = System.getProperty("user.home");//home directory
            userDirectory = new File(userDirectoryString);
            if(!userDirectory.canRead()) {
                userDirectory = new File("C:/");
            }
        }
            //set default directory
        fc.setInitialDirectory(userDirectory);



        file = fc.showOpenDialog(null);

        if (file != null){
            labChooseFile.setText(file.getName());
            try (OutputStream output = new FileOutputStream(fileName)) {

                Properties propOut = new Properties();

                if(file.canRead()) {
                    //System.out.println(file.getPath());
                    propOut.setProperty("path.to.hex", file.getPath());
                    // save properties to project root folder
                    propOut.store(output, null);

                }



            } catch (IOException io) {
                io.printStackTrace();
            }
        }



    }

    @FXML
    public void convertHexToHes() throws IOException {

        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        }
        catch(Exception e){
            labErrorMsg.setText("Please select a file!");
        }

        if(scanner != null){//there must be file
            String prevLine = "";//to check if line is part of a certain block
            String line = null;
            String outputLine = "";
            String msbOutput = txtFromAddress.getText().substring(4,6);
            long adrOutput = Long.parseLong(txtFromAddress.getText().substring(6,10), 16);
            msb = txtFromAddress.getText().substring(2,6);
            lsbFrom = txtFromAddress.getText().substring(6,10);
            lsbTo  = txtToAddress.getText().substring(6,10);
            boolean bool = true;
            File outputFile = new File("output.hes");
            PrintWriter writer = new PrintWriter(new FileWriter(outputFile,true));
            writer.println(":070000000001FFFFFFFFFFFFFF ");

            while(scanner.hasNext()){//looping until end of file

                line = scanner.nextLine();

                if(line.matches("^:02.{4}04"+msb+".{2}$")){
                    int l = 0;
                    int length = 0;
                    int address = 0;
                    int prevAddress = 999999;
                    String data = "";

                    while(bool){
                        line = scanner.nextLine();
                        if(line.matches("^:.{2}"+lsbFrom+"00.*$")){
                            while(!(line.matches("^:.{2}"+lsbTo+"00.*$"))){
                                //writer.println(line);
                                boolean nextLineSetted = false;
                                if(line.matches("^:.{6}04.*$")){
                                    break;
                                }
                                length = Integer.parseInt(line.substring(1, 3), 16);
                                address = Integer.parseInt(line.substring(3, 7), 16);
                                data = line.substring(9, 9+(length*2));
                                //System.out.println("len: "+length);
                                //System.out.println("adr: "+address);
                                //System.out.println("data: "+data);
                                int diff = address-prevAddress;

                                if( diff > 16){
                                    diff = (diff/16)-1;
                                    for(int i = 0;  i < diff; i++){
                                        outputLine = outputLine+"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
                                        l = l + 16;
                                        if(l == 128){
                                            calculateCheckSum(":82"+String.format("%04X", adrOutput)+"00"+msbOutput+"02"+outputLine);
                                            writer.println(lineCheckSum);
                                            l = 0;
                                            adrOutput = adrOutput + 128;
                                            outputLine = "";

                                        }
                                    }
                                }

                                if(length != 16){
                                    line = scanner.nextLine();
                                    nextLineSetted = true;
                                    if(Integer.parseInt(line.substring(7, 9), 16) != 0){
                                        while(length < 16){
                                            data = data+"FF";
                                            length = length + 1;
                                        }
                                    }
                                    else {
                                        int nextAddress = Integer.parseInt(line.substring(3, 7), 16);
                                        while(length < (nextAddress - address)){
                                            data = data+"FF";
                                            length = length + 1;
                                        }
                                    }

                                }
                                if(l < 128){
                                    outputLine = outputLine + data;
                                    l = l + length;
                                }
                                if(l == 128){
                                    calculateCheckSum(":82"+String.format("%04X", adrOutput)+"00"+msbOutput+"02"+outputLine);
                                    writer.println(lineCheckSum);
                                    l = 0;
                                    adrOutput = adrOutput + 128;
                                    outputLine = "";

                                }
                                ////////////////////////////////
                                if(!nextLineSetted){
                                    line = scanner.nextLine();
                                }
                                data = "";
                                prevAddress = address;
                            }
                            //writer.println(line);//to print last line of given sequence
                            if(l > 0){
                                while(l < 128){
                                    outputLine = outputLine+"FF";
                                    l = l + 1;
                                }
                                calculateCheckSum(":82"+String.format("%04X", adrOutput)+"00"+msbOutput+"02"+outputLine);
                                writer.println(lineCheckSum);
                                l = 0;
                                outputLine = "";
                            }

                         bool = false;
                        }
                    }

                }
                //:020000040020DA
                if((prevLine.length() == 15) && prevLine.matches("^:02.{4}040031.{2}$")){
                    softVersion(line);
                    header = true;
                }

                if((prevLine.length() == 15) && prevLine.matches("^:02.{4}040020.{2}$")){
                    scanInfo(line);
                    header = true;
                }

                prevLine = line;
            }
            if(invalid == false && header == true){
                if(versionHex != null ){
                    calculateCheckSum(":050003003103" + versionHex);
                    writer.println(lineCheckSum);//pre-last line
                }
                writer.print(":020000001F00DF");//last line

                String deviceName = "" ;
                if(typeDevice == "UN") {deviceName = "DC40_" + typeDevice;}
                if(typeDevice == "PB" || typeDevice == "PP"){ deviceName = "C150_" + typeDevice;}

                String fileName = deviceName+"_hw"+hardwareVersion+"_"+versionString+"_"+formatter.format(date)+".hes";
                File renamedFile = new File(fileName);
                writer.close();
                renamedFile.delete();
                outputFile.renameTo(renamedFile);
                labOutputFile.setText(fileName);

                RandomAccessFile f = new RandomAccessFile(new File(fileName), "rw");
                calculateCheckSum(":070000000001"+versionHex+typeHex(typeDevice)+hardwareAscii);
                f.seek(0); // to the beginning

                f.write(lineCheckSum.getBytes());
                f.close();
            }
            else{
                if(header == false){
                    labErrorMsg.setText("Input file is not compatible!");
                }
                writer.close();
                outputFile.delete();
            }
        }


    }
    void softVersion(String line){
        if(!(line.matches("^:10000000.{12}43313530.*$")) && !(line.matches("^:10000000.{12}44433430.*$"))) {
            labErrorMsg.setText("Input file is not compatible!");
            invalid = true;
        }
        else{

            //check the version
            versionHex = line.substring(15,21);
            int byte1 = Integer.parseInt(line.substring(15, 17), 16);// address 0310003
            int byte2 = Integer.parseInt(line.substring(17, 19), 16);// address 0310004
            int byte3 = Integer.parseInt(line.substring(19, 21), 16);// address 0310005
            versionString = byte1 +"."+ byte2 + byte3;
            //versionAscii = String.valueOf(byte1)+String.valueOf(byte2)+String.valueOf(byte3);
            labVersion.setText(versionString);
        }

    }

    void scanInfo(String line){


        if(!(line.matches("^:10000000.*$"))){
            labErrorMsg.setText("Input file is not compatible!");
            invalid = true;
        }
        else{
            //: 0B- „PB“, 0C-„PP“, 0F-"UN"
            //check the type of device
            typeDevice = line.substring(11, 13);// address 00310002
            hardwareVersion = line.substring(13, 15);
            int byte1 = Integer.parseInt(line.substring(13, 14), 16);// address 0200003
            int byte2 = Integer.parseInt(line.substring(14, 15), 16);// address 0200004
            if(byte1 < 10) {
                byte1 = byte1+30;
            }
            else{
                byte1 = byte1+31;
            }

            if(byte2 < 10) {
                byte2 = byte2+30;
            }
            else{
                byte2 = byte2+31;
            }
            hardwareAscii = String.valueOf(byte1)+String.valueOf(byte2);;
            int hv = Integer.parseInt(hardwareVersion, 16);
            hardwareVersion= String.format("%02d", hv);
            //System.out.println(hardwareVersion);
            int td= Integer.parseInt(typeDevice, 16);

            switch (td){
                case 11:
                    labType.setText("C150_PB_" + "hw" +  hardwareVersion);
                    typeDevice = "PB";
                    break;
                case 12:
                    labType.setText("C150_PP_" + "hw" +  hardwareVersion);
                    typeDevice = "PP";
                    break;
                case 15:
                    labType.setText("DC40_UN_" + "hw" +  hardwareVersion);
                    typeDevice = "UN";
                    break;
                default:
                    labErrorMsg.setText("Type of device is not compatible!");
                    break;
                }

        }
    }

    void calculateCheckSum(String line){
        lineCheckSum = "";
        int checkSum = 0;
        int oneByte;//two characters
        String hexSum;

        for(int i = 1; i < line.length(); i = i+2){
            oneByte = Integer.parseInt(line.substring(i, i+2), 16);
            //System.out.println(oneByte);
            checkSum = checkSum + oneByte;
        }
        //System.out.println(checkSum);
        // two complement
        if(checkSum <= 256){//in hex 100
            checkSum = 256 - checkSum;
        }
        else{// in hex 1000
            checkSum = 4096 - checkSum;
        }
        hexSum = Integer.toHexString(checkSum).toUpperCase();//from int to uppercase  hex string
        hexSum = hexSum.substring(hexSum.length() - 2);
        //System.out.println(hexSum);
        lineCheckSum = line + hexSum;//concatenate string
        //System.out.println(lineCheckSum);
    }

    String typeHex (String asciiToHex){
        char[] ch = asciiToHex.toCharArray();
        // Iterate over char array and cast each element to Integer.
        StringBuilder builder = new StringBuilder();
        for (char c : ch) {
            int i = (int) c;
            //Convert integer value to hex using toHexString() method.
            builder.append(Integer.toHexString(i).toUpperCase());
        }
        return builder.toString();
    }
}
