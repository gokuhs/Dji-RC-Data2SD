package eu.gokuhs.djircdata2sd;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import dadb.AdbKeyPair;
import dadb.AdbShellResponse;
import dadb.Dadb;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private boolean rootOnAdb = false;
    private boolean hasRoot = false;

    private String SDPartition = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Switch showDebugSw = (Switch)findViewById(R.id.showDebug);
        showDebugSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showHideDebugText(isChecked);
            }
        });
        showHideDebugText(showDebugSw.isChecked());
        startApp();
    }
    protected  void startApp(){
        String deviceInfo = getProp("ro.product.odm.device", "");
        debug("Detected device: " +deviceInfo);
        if(!deviceInfo.equals("rm330")){
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name))
                    .setMessage(getString(R.string.wrong_device))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            findRoot();
                            checkSDExtPartition();
                            controlSD();
                        }})

                    .setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finishAffinity();
                        }}).show();;
        } else {
            findRoot();
            checkSDExtPartition();
            controlSD();
        }

    }

    private void blockUi(){
        this.enableDisableButton(false);
        this.enableDisableSDButton(false);
    }
    private void checkEnabledButtonMagic(){
        if (!SDPartition.isEmpty() && hasRoot){
            this.enableDisableButton(true);
        } else {
            this.enableDisableButton(false);
        }
    }

    private void findRoot(){
        CheckBox checkRoot =  (CheckBox) findViewById(R.id.root);
        adbExecutionInterface functionCommand = (String returned) -> {
            if (returned.equals("0")) {
                this.rootOnAdb = false;
                this.hasRoot = true;
                checkRoot.setChecked(true);
                this.checkEnabledButtonMagic();
            }
            else {
                this.ExecuteOnADB("id -u", (String returnedAdb) -> {
                    if (returnedAdb.equals("0\n")) {
                        this.rootOnAdb = true;
                        this.hasRoot = true;
                        checkRoot.setChecked(true);
                        this.checkEnabledButtonMagic();
                    }
                    else {
                        debug("No root detected!");
                        new AlertDialog.Builder(this)
                                .setTitle(getString(R.string.app_name))
                                .setMessage(getString(R.string.no_root_detected))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, null).show();
                    }
                });
            }
        };

        this.executeCommand("id -u", functionCommand);

    }

    public void executeAsRoot(String command, adbExecutionInterface func) {
        if (hasRoot){
            if (rootOnAdb){
                this.ExecuteOnADB(command, func);
            }
            else {
                this.executeCommand(command, func);
            }
        }
    }

    public interface adbExecutionInterface {
        void postExecution(String stdOut);
    }

    private void ExecuteOnADB(String command, adbExecutionInterface func){
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    //Generate Keys file
                    File privatekey = new File(getApplication().getExternalCacheDir() + "/adbPrivate");
                    File publickey = new File(getApplication().getExternalCacheDir() + "/adbPublic");
                    if (!privatekey.exists()){
                        AdbKeyPair.generate(privatekey,publickey);
                    }
                    var adbKeyPair = AdbKeyPair.read(privatekey, publickey);

                    Dadb adb = Dadb.create("127.0.0.1", getAdbTcpPort(), adbKeyPair);
                    debug("adb > " + command);
                    AdbShellResponse response = adb.shell(command);
                    String result = response.getAllOutput();
                    debug(result);
                    return  result;
                } catch (Exception e) {
                    debug( "error");
                    func.postExecution(null);
                }
                return "";
            }
            @Override
            protected void onPostExecute(String result) {
                func.postExecution(result);
            }

        }.execute();
    }

    public void showHideDebugText(boolean showHide){
        TextView t = (TextView) findViewById(R.id.debugView);
        t.setVisibility(showHide ? View.VISIBLE : View.GONE);
    }
    
    public void enableDisableButton(boolean status){
        Button button = (Button) findViewById(R.id.button);
        button.setEnabled(status);
    }

    public void enableDisableSDButton(boolean status) {
        Button button = (Button) findViewById(R.id.buttonPrepareSD);
        button.setEnabled(status);
    }
    public void checkSDExtPartition(){
        this.executeCommand("ls -al /dev/block/mmcblk1p* | wc -l", (String returned) -> {
            if(returned.equals("3")){
                SDPartition = "3";
            } else if (returned.equals("2")) {
                SDPartition = "2";
            }
            debug("Detected parttion: " + SDPartition);
            //this.enableDisableSDButton(SDPartition.isEmpty());
            this.enableDisableSDButton(true);
            checkEnabledButtonMagic();
        });
    }



    private void controlSD(){
        this.executeCommand("mount | grep mmcblk1p", (String returned) -> {
            if(!returned.equals("\n") && !returned.isEmpty()){
                this.blockUi();
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name))
                        .setMessage(getString(R.string.sd_mounted))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finishAffinity();
                            }}).show();
            }
            else {
                checkHasSD();
            }
        });
    }

    private void checkHasSD(){
        this.executeCommand("ls -l /dev/block/mmcblk1* | wc -l", (String returned) -> {
            if(returned.equals("0")){
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name))
                    .setMessage(getString(R.string.no_SD))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finishAffinity();
                        }}).show();
            }
        });
    }

    public void doMagic(View v)
    {
        try{
            this.executeDATA2SD(v);
            //this.finishAffinity();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    String getcommands(){
        return "#!/system/bin/sh\n" +
                "# mount /data to SD\n" +
                "sddata=/dev/block/mmcblk1p" + this.SDPartition+ "\n" +
                "if [ -e  ];\n" +
                "then\n" +
                "umount /data/media\n" +
                "umount /data/media\n" +
                "umount /data/media\n" +
                "umount /data/media\n" +
                "umount /data/media\n" +
                "umount /data/media\n" +
                "umount -l /data\n" +
                "mount /dev/block/mmcblk1p" + this.SDPartition+ " /data\n" +
                "mount -t sdcardfs /data/media /mnt/runtime/default/emulated\n" +
                "mount -t sdcardfs /data/media /storage/emulated \n" +
                "mount -t sdcardfs /data/media /mnt/runtime/read/emulated \n" +
                "mount -t sdcardfs /data/media /mnt/runtime/write/emulated\n" +
                "mount -t sdcardfs /data/media /mnt/runtime/full/emulated\n" +
                "am restart\n" +
                "else \n" +
                "echo NO SD\n" +
                "fi\n";
    }

    void executeCommand(String command, adbExecutionInterface func){
        try {
            String line;
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            stdin.flush();

            stdin.close();
            debug(" > " + command);
            String out = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                debug(line);
                out += line;
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                debug("[Error]" + line);
            }
            br.close();

            process.waitFor();
            process.destroy();
            func.postExecution(out);
        } catch (Exception e) {
            this.debug("I can't execute the command: "+ command);
            func.postExecution(null);
        }
    }

    private void partitionSDProcess(String diskName){
        blockUi();
        ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.sdPartition));
        mProgressDialog.setMessage(getString(R.string.partition_1_partitioning));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
        String partition = "sm partition " + diskName.replace('\n', ' ') +"mixed 50";
        executeAsRoot(partition, (String partitionOk) -> {
            mProgressDialog.setMessage(getString(R.string.partition_2_unmount));
            mProgressDialog.setProgress(5);
            String partitionUmount = "sm forget all";
            executeAsRoot(partitionUmount, (String partitionOk2) -> {
                mProgressDialog.setProgress(10);
                mProgressDialog.setMessage(getString(R.string.partition_3_wait));
                executeAsRoot("sleep 30", (String dummy) -> {
                    mProgressDialog.setProgress(40);
                    mProgressDialog.setMessage(getString(R.string.partition_4_format));
                    String partitionFormat= "mkfs.ext4 /dev/block/mmcblk1p3";
                    executeAsRoot(partitionFormat, (String partitionFormatRes) -> {
                        mProgressDialog.setProgress(50);
                        mProgressDialog.setMessage(getString(R.string.partition_5_mount));
                        executeAsRoot("mount /dev/block/mmcblk1p3 /mnt/expand", (String externalDisk) -> {
                            mProgressDialog.setProgress(55);

                            mProgressDialog.setMessage(getString(R.string.partition_6_copy));
                            executeAsRoot("cp -rvp /data/ /mnt/expand" , (String filesCopied) -> {
                                mProgressDialog.setProgress(100);

                                mProgressDialog.setMessage(getString(R.string.partition_7_reboot));
                                executeAsRoot("sleep 5; reboot", (String omit) -> {
                                    mProgressDialog.hide();
                                } );
                            });
                        });
                    });
                });
            });
        });

    }

    public void prepareSD( View view){
        String sdDisk = "sm list-disks";
        executeAsRoot(sdDisk, (String diskName) -> {
            if(!diskName.isEmpty()) {
                 new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(getString(R.string.confirmPartitionSD))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(MainActivity.this, getString(R.string.doPartitionToast), Toast.LENGTH_SHORT).show();
                        partitionSDProcess(diskName);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
            } else {
                debug("Micro SD not detected!");
            }
        });
    }
    void executeDATA2SD(View view) throws Exception {
        if (this.hasRoot){
            debug("Executing data2SD in the std shell");
            ProgressDialog mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(getString(R.string.mounting_title));
            mProgressDialog.setMessage(getString(R.string.mounting_content));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
            this.executeAsRoot(this.getcommands(), (String out) -> {});
        }
    }

    void debug(String text)
    {
        TextView t = (TextView) findViewById(R.id.debugView);
        t.append(text + "\n\r");
    }

    int getAdbTcpPort() {
        var port = this.getProp("service.adb.tcp.port", "-1");
        if (port == "-1") port = this.getProp("persist.adb.tcp.port", "-1");
        return Integer.parseInt(port);
    }


    private static boolean failedUsingReflection = false;
    private static Method getPropMethod = null;

    //@SuppressLint("PrivateApi")
    public static String getProp(String propName, String defaultResult) {
        if (defaultResult == null) {
            defaultResult = "";
        }
        if (!failedUsingReflection) {
            try {
                if (getPropMethod == null) {
                    Class<?> clazz = Class.forName("android.os.SystemProperties");
                    getPropMethod = clazz.getMethod("get", String.class, String.class);
                }
                return (String) getPropMethod.invoke(null, propName, defaultResult);
            } catch (Exception e) {
                getPropMethod = null;
                failedUsingReflection = true;
            }
        }
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("getprop " + propName + " " + defaultResult);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (IOException e) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return defaultResult;
    }
}