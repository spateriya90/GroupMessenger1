package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private static int count = 0;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //Code taken from PA1 for getting current AVD port number
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //Code taken from PA1 to create server socket
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(10000);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            //Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         */

        //Referenced from https://developer.android.com/guide/topics/ui/controls/button.html#ClickListener
        // https://developer.android.com/reference/android/widget/EditText.html
        final EditText ed = (EditText) findViewById(R.id.editText1);
        Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = String.valueOf(ed.getText()+"\n");
                tv.append(s);   //Display message on sender screen and clear the textbox
                ed.setText("");
                //Code below taken from PA1
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s, myPort);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    //Code below taken from PA1, with changes added to send values to the content provider


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            try {

                while (true) {
                    Socket s = serverSocket.accept();

                    InputStreamReader is = null;

                    is = new InputStreamReader(s.getInputStream());

                    BufferedReader br = new BufferedReader(is);
                    String msg;
                    if ((msg = br.readLine()) != null) {
                        //count++;
                        String URL = "content://edu.buffalo.cse.cse486586.groupmessenger1.provider";
                        //content://edu.buffalo.cse.cse486586.groupmessenger1.provider
                        Uri uri = Uri.parse(URL);
                        ContentValues values = new ContentValues();
                        values.put(KEY_FIELD,count);
                        values.put(VALUE_FIELD,msg);
                        Uri newUri = getContentResolver().insert(uri,values);
                        publishProgress(msg);
                        count++;     //Count starts from 0 and hence should be incremented after insert
                        br.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */


            //Display received message in textView1
            String strReceived = strings[0].trim();
            TextView tv1 = (TextView) findViewById(R.id.textView1);
            //remoteTextView.append(strReceived + "\t\n");
            //TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            tv1.append(strReceived+"\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

//            String filename = "SimpleMessengerOutput";
//            String string = strReceived + "\n";
//            FileOutputStream outputStream;
//
//            try {
//                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//                outputStream.write(string.getBytes());
//                outputStream.close();
//            } catch (Exception e) {
//                //  Log.e(TAG, "File write failed");
//            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            //Create an array of port numbers for the 5 AVDs
            int[] remotePorts = {11108, 11112, 11116, 11120, 11124};
            try {

                for (Integer i : remotePorts) {
                    //String remotePort = REMOTE_PORT0;
                    //if (msgs[1].equals(REMOTE_PORT0))
                    //  remotePort = REMOTE_PORT1;

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            i);

                    String msgToSend = msgs[0];
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    // BufferedWriter bw = new BufferedWriter(socket.getOutputStream());
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    pw.println(msgToSend);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    socket.close();
                }
            } catch (UnknownHostException e) {
                // Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                //Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
