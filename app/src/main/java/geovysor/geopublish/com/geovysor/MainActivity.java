package geovysor.geopublish.com.geovysor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import db.SQLServerConnection;

public class MainActivity extends AppCompatActivity {

    Button btnAccess;
    EditText txtClientCode;
    EditText txtPromotionCode;
    SQLServerConnection conn;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Verificando...");
        dialog.setTitle("GeoVysor");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        setContentView(R.layout.activity_main);

        btnAccess=(Button)findViewById(R.id.btnAccess);
        txtClientCode=(EditText) findViewById(R.id.txtClientCode);
        txtPromotionCode=(EditText) findViewById(R.id.txtPromotionCode);

        btnAccess.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        BackgroundTask task = new BackgroundTask();
                        task.execute(txtClientCode.getText().toString(), txtPromotionCode.getText().toString());

                        /*
                        try {
                            task.execute(txtClientCode.getText().toString(), txtPromotionCode.getText().toString()).get(5000, TimeUnit.MILLISECONDS);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Se interrumpió la conexión", Toast.LENGTH_SHORT).show();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Hubo un error en la aplicación. Por favor comuníquese con soporte técnico", Toast.LENGTH_SHORT).show();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Se supero el tiempo de espera para conectarse a la base de datos Geopublish", Toast.LENGTH_SHORT).show();
                        }
                        */

                    }
                }
        );
    }

    private class BackgroundTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            dialog.show(); //Mostramos el diálogo antes de comenzar
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String clientCode, clientPromo;

            clientCode=params[0].toString();
            clientPromo=params[1].toString();

            conn = new SQLServerConnection();

            return (conn.ExistsClientPromo(clientCode,clientPromo));
        }


        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (result)
            {
                Intent statsIntent=new Intent(MainActivity.this, StatsActivity.class);

                statsIntent.putExtra("CLIENT_CODE", txtClientCode.getText().toString());
                statsIntent.putExtra("PROMOTION_CODE", txtPromotionCode.getText().toString());

                startActivity(statsIntent);
                finish();
            }
            else
                Toast.makeText(MainActivity.this, "No se encuentran los datos del cliente o de la promoción", Toast.LENGTH_SHORT).show();
        }


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
