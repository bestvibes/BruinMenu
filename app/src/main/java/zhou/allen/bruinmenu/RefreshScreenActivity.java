package zhou.allen.bruinmenu;

/**
 * Created by Owner on 10/18/2015.
 */

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RefreshScreenActivity extends Activity
{
    //A ProgressDialog object
    private ProgressDialog progressDialog;

    ArrayList<String> favoriteFoodPresent;
    ArrayList<String> favoriteFood;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Initialize a LoadViewTask object and call the execute() method
        new GetPageTask().execute();


    }

    //To use the AsyncTask, it must be subclassed
    private class GetPageTask extends AsyncTask<Void, Integer, Void> {
        //String html;
        boolean refresh;
        boolean error=false;
        //Before running code in separate thread
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(RefreshScreenActivity.this, "Connecting...",
                    "Connecting to Bruin Menu, please wait...", false, false);
        }

        //The code to be executed in a background thread.
        @Override
        protected Void doInBackground(Void... params) {
            //Get the current thread's token
            synchronized (this) {
                try {
                    OkHttpClient client = new OkHttpClient();

                    String url = "http://menu.ha.ucla.edu/foodpro/default.asp";
                    client.setConnectTimeout(15, TimeUnit.SECONDS); // connect timeout
                    client.setReadTimeout(15, TimeUnit.SECONDS);    // socket timeout
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    //Response response = client.newCall(request).execute();
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    String html = response.body().string();

                    response.body().close();


                    //((AppVariables) getApplicationContext()).setBruinMenu(html);
                    //System.out.print("I got the page!");

                    MenuDBHelper dbHelper = new MenuDBHelper(getApplicationContext());

                    favoriteFoodPresent = new ArrayList<>();
                    favoriteFood = (ArrayList<String>) dbHelper.getFavorites();
                    // Get the database. If it does not exist, this is where it will
                    // also be created.
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    db.delete(MenuDBContract.HallEntry.TABLE_NAME, null, null);
                    db.delete(MenuDBContract.KitchenEntry.TABLE_NAME, null, null);
                    db.delete(MenuDBContract.MenuEntry.TABLE_NAME, null, null);

                    Document doc = Jsoup.parse(html);
                    Elements menus = doc.getElementsByClass("menucontent");

                    for (int i = 0; i < menus.size(); i++) {
                        String mealTime;
                        Elements timeMenu = menus.get(i).select(".menumealheader");
                        if (timeMenu.first().text().toLowerCase().contains("breakfast"))
                            mealTime = "breakfast";
                        else if (timeMenu.first().text().toLowerCase().contains("lunch"))
                            mealTime = "lunch";
                        else
                            mealTime = "dinner";

                        //Elements halls = menus.get(i).select(".menulocheader");
                        //Elements leftCells = menus.get(i).select(".menugridcell, .menusplit");
                        //Elements rightCells = menus.get(i).select(".menugridcell_last, .menusplit");
                        Elements cells = menus.get(i).select(".menulocheader, .menugridcell, .menugridcell_last, .menusplit");

                        ArrayList<Long> hallsIds = new ArrayList<>();
                        int ite = 0;
                        boolean splitEncountered = false;
                        int numCols = 0;
                        int splits = 0;

                        for (Element cell : cells) {
                            if (cell.hasClass("menulocheader")) {
                                if (!splitEncountered) {
                                    numCols++;
                                }
                                ContentValues values = new ContentValues();
                                values.put(MenuDBContract.HallEntry.COLUMN_NAME_ITEM, cell.text().trim());
                                values.put(MenuDBContract.HallEntry.COLUMN_NAME_MEALTIME, mealTime);
                                hallsIds.add(db.insert(
                                        MenuDBContract.HallEntry.TABLE_NAME,
                                        null,
                                        values));
                            }
                            else if (cell.hasClass("menusplit")) {
                                splitEncountered = true;
                                splits++;
                                ite = numCols * splits;
                            }
                            else {
                                if (ite >= hallsIds.size()) {
                                    continue;
                                }
                                Elements kitchen = cell.select(".category5");
                                ContentValues kvalues = new ContentValues();
                                if(kitchen.first() == null) continue;

                                String kitchenName = kitchen.first().text().trim();
                                kvalues.put(MenuDBContract.KitchenEntry.COLUMN_NAME_ITEM, kitchenName);
                                kvalues.put(MenuDBContract.KitchenEntry.COLUMN_NAME_HALL, hallsIds.get(ite));
                                long id = db.insert(
                                        MenuDBContract.KitchenEntry.TABLE_NAME,
                                        null,
                                        kvalues);

                                Elements items = cell.select(".level5");
                                for (Element e : items) {
                                    ContentValues ivalues = new ContentValues();
                                    Element link = e.select("a").first();
                                    if(e == null) continue;

                                    String menuItemName = e.text().trim();
                                    ivalues.put(MenuDBContract.MenuEntry.COLUMN_NAME_ITEM, menuItemName);
                                    ivalues.put(MenuDBContract.MenuEntry.COLUMN_NAME_KITCHEN, id);
                                    ivalues.put(MenuDBContract.MenuEntry.COLUMN_NAME_NUTRIURL, link.attr("href"));
                                    if(favoriteFood.contains(menuItemName)) {
                                        favoriteFoodPresent.add(kitchenName + "-" + menuItemName);
                                    }

                                    Element v = e.select("img").first();
                                    int veg = 0;
                                    if (v == null) {
                                        veg = 0;
                                    } else if (v.attr("alt").toLowerCase().contains("vegetarian")) {
                                        veg = 1;
                                    } else if (v.attr("alt").toLowerCase().contains("vegan")) {
                                        veg = 2;
                                    }
                                    ivalues.put(MenuDBContract.MenuEntry.COLUMN_NAME_VEG, veg);
                                    long id2 = db.insert(
                                            MenuDBContract.MenuEntry.TABLE_NAME,
                                            null,
                                            ivalues);
                                }
                                ite++;
                                if (ite >= numCols * (splits + 1)) {
                                    ite -= numCols;
                                }
                            }
                        }
                    }
                    dbHelper.close();
                    db.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    error = true; //TODO: TEMP FIX
                }
            }

            //checking if notifications should display
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean notification_switch = prefs.getBoolean("notification_switch_refresh", false);
            if(notification_switch) {
                Context _context = getApplicationContext();
                //displaying notification
                if (!favoriteFoodPresent.isEmpty()) {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(_context).
                            setSmallIcon(R.drawable.notification).
                            setLargeIcon(BitmapFactory.decodeResource(_context.getResources(), R.drawable.notification)).
                            setContentTitle("Today's Favorites").
                            setContentText(favoriteFoodPresent.get(0) + (favoriteFoodPresent.size() == 1 ? "" : "...."));
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    inboxStyle.setBigContentTitle("Today's Favorites");
                    for (String foods : favoriteFoodPresent) {
                        inboxStyle.addLine(foods);
                    }
                    builder.setStyle(inboxStyle);
                    builder.setContentIntent(PendingIntent.getActivity(_context, 0, new Intent(_context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
                    builder.setAutoCancel(true);
                    NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    int notificationID = 1;
                    notifManager.notify(notificationID, builder.build());
                }
            }

            return null;
        }


        /*
        //Update the progress
        @Override
        protected void onProgressUpdate(Integer... values) {
            //set the current progress of the progress dialog
            progressDialog.setProgress(values[0]);
        }
        */

        //after executing the code in the thread
        @Override
        protected void onPostExecute(Void result) {
            if(error) {
                progressDialog = ProgressDialog.show(RefreshScreenActivity.this, "Connection Failed",
                        "Unable to connect to BruinMenu", false, false);
                try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            //close the progress dialog
            progressDialog.dismiss();

            Intent i = new Intent(RefreshScreenActivity.this, MainActivity.class);
            startActivity(i);
        }
    }
}