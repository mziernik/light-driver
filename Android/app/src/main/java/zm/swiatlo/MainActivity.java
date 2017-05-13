package zm.swiatlo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import http.websocket.client.WebSocketClient;
import http.websocket.handshake.ServerHandshake;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // wyświetlanie hinta
        // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //         .setAction("Action", null).show();


        new Obliczenia(this).execute();


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

class Obliczenia extends AsyncTask<Void, String, Void> {

    Activity activity;
    ProgressDialog pd;

    public Obliczenia(Activity activity) {
        this.activity = activity;

        ConnectivityManager cm = (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);


        // NetworkInfo info = cm.getActiveNetworkInfo();
        TelephonyManager tm = (TelephonyManager) this.activity.getSystemService(Context.TELEPHONY_SERVICE);

        tm.getNetworkType();
        //   info.getType();
        //  info.getTypeName();

        //Toast.makeText(this.activity, "Połączenie: " + info.getTypeName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        pd.setMessage(values[0]);
    }


    @Override

    protected void onPreExecute() {
        pd = new ProgressDialog(activity);
        pd.setMessage("loading");
        pd.show();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            new WsClient().connect();

            synchronized (WsClient.groups) {
                WsClient.groups.wait(10000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    @Override
    protected void onPostExecute(Void result) {
        ScrollView lMain = (ScrollView) activity.findViewById(R.id.content);


        TableLayout table = new TableLayout(activity);
        table.setColumnStretchable(0, true);
        lMain.addView(table);


        for (WsClient.Group group : WsClient.groups) {
            TableRow row = new TableRow(activity);
            table.addView(row);

            {
                TextView text = new TextView(activity);
                text.setText(group.name);
                row.addView(text);
            }
            {
                TextView text = new TextView(activity);
                text.setText((group.value) + " %");
                text.setGravity(Gravity.RIGHT);
                row.addView(text);
            }

            SeekBar seek = new SeekBar(activity);
            row = new TableRow(activity);
            table.addView(row);


            TableRow.LayoutParams pars = new TableRow.LayoutParams();
            pars.span = 2;
            pars.bottomMargin = 15;
            row.addView(seek, pars);
        }


        pd.hide();
    }

}