package me.scryptminers.android.incognito.Service;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.scryptminers.android.incognito.Activity.ChatActivity;
import me.scryptminers.android.incognito.Activity.GroupChatActivity;
import me.scryptminers.android.incognito.Database.ChatDatabaseHelper;
import me.scryptminers.android.incognito.Model.Group;
import me.scryptminers.android.incognito.Model.GroupMessage;
import me.scryptminers.android.incognito.Model.Message;
import me.scryptminers.android.incognito.Util.PGP;
import me.scryptminers.android.incognito.Util.SharedValues;

/**
 * Created by Samruddhi on 5/2/2017.
 */

public class GroupMessageService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    private String groupName, userEmail;
    private Handler handler = new Handler();
    private static int lastread=0;
    private Runnable runnableCode = new Runnable() {

        @Override
        public void run() {

            ReceiveGroupMessageTask receiveGroupMessageTask = new ReceiveGroupMessageTask();
            receiveGroupMessageTask.execute("dsds");
            //ChatDatabaseHelper db = new ChatDatabaseHelper(getApplicationContext());
            //db.getAllMessages(userEmail);

        }
    };

    public class ReceiveGroupMessageTask extends AsyncTask<String, Void, Boolean> {

        private RequestQueue requestQueue;
        private JsonObjectRequest jsonObjectRequest;
        private final String URL="https://scryptminers.me/getTeamMessage";
        private boolean successrun;

        public ReceiveGroupMessageTask() {
        }

        @Override
        protected Boolean doInBackground(String... params) {

            Map<String,String> userMap = new HashMap<>();
            userMap.put("to", SharedValues.getValue("USER_EMAIL"));
            userMap.put("group_name", groupName);
            userMap.put("Last_Group_Read",""+SharedValues.getLong("Last_Read"+groupName));
            try {
                // Simulate network access.
                requestQueue = Volley.newRequestQueue(getApplicationContext());
                jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, new JSONObject(userMap), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("messages");
                            if(jsonArray.length()==0)
                            {
                                successrun=false;
                            }
                            else {
                                //String message_received  = response.getString("message");
                                successrun=true;
                                ChatDatabaseHelper db = new ChatDatabaseHelper(getApplicationContext());
                                for (int i = 0, size = jsonArray.length(); i < size; i++) {
                                    JSONObject objectInArray = jsonArray.getJSONObject(i);
                                    String ciphertext = objectInArray.getString("message");
                                    String message = PGP.decryptMessage(ciphertext, SharedValues.getValue("PRIVATE_KEY"));
                                    String direction="right";
                                    db.addGroupMessage(new GroupMessage(message,objectInArray.getString("from"),userEmail,groupName,direction));
                                    GroupChatActivity.groupMessages.add(new GroupMessage(message,objectInArray.getString("from"),userEmail,groupName,direction));
                                    Log.d("Recieved", message);
                                    SharedValues.save("Last_Read"+groupName, objectInArray.getInt("id"));


                                    /*for(int j = 0;j<groupMembers.length;j++){
                                        if(!groupMembers[j].matches(userEmail)){
                                            //position = j;
                                        //String cipherToDecrypt = arrCiphertext[position];

                                        }
                                    }*/
                                    /*//String groupKey = db.getGroupKey(groupName);
                                    String groupKey=SharedValues.getValue(groupName+"_KEY");
                                    //--------------------------------------------------------------
                                   // byte[] encodedKey     = android.util.Base64.decode(groupKey, android.util.Base64.DEFAULT);
                                   // SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
                                    //--------------------------------------------------------------
                                    byte[] gkey = Base64.decode(groupKey);
                                    SecretKey encryptionKey = new SecretKeySpec(gkey, "AES");
                                    byte[] rawAESkey = encryptionKey.getEncoded();
                                    //byte[] decryptedMessage = PGP.decryptGroupMessage(ciphertext, encryptionKey);
                                    Log.e("Received groupc",ciphertext);
                                    byte[] decryptedMessage = PGP.decryptGroupMessage(ciphertext, encryptionKey);
                                    //String message = PGP.decryptGroupMessage(ciphertext, rawAESkey);
                                    String message = new String(decryptedMessage);
                                    //db.addMessage(new Message(friendEmail, userEmail, message, "right"));

                                  */
                                    //msgs.add(new Message(friendName,message,"right"));
                                }
                                //listViewChat.invalidate();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                requestQueue.add(jsonObjectRequest);
                while (!jsonObjectRequest.hasHadResponseDelivered())
                    Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.d("error",e.toString());
            }

            return successrun;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                sendBroadcast(new Intent("UpdateGroupMessage"));
            } else {

            }
            handler.postDelayed(runnableCode,3000);
        }

        @Override
        protected void onCancelled() {
        }
    }

    public GroupMessageService() {
        super("MyGroupMessageService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        groupName = intent.getStringExtra("GROUP_NAME");
        userEmail = SharedValues.getValue("USER_EMAIL");
        handler.post(runnableCode);
    }
}
