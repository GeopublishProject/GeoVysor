package geovysor.geopublish.com.geovysor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;


import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import db.SQLServerConnection;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;



public class StatsActivity extends AppCompatActivity {

    private SQLServerConnection conn;
    private ComboLineColumnChartView chart;
    private int numberOfLines = 1;
    private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasPoints = true;
    private boolean hasLines = true;
    private boolean isCubic = false;
    private boolean hasLabels = false;
    private MapView mapView;
    private TileCache tileCache;
    private TileRendererLayer tileRendererLayer;
    private Handler handler = new Handler();
    private BarChart barChart;
    private Bundle extras;
    private TextView txtReportName;
    private Marker busMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        AndroidGraphicFactory.createInstance(getApplication());
        setContentView(R.layout.activity_stats);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
        }

        //Map
        try
        {
            this.mapView = (MapView)findViewById(R.id.mapView);

            this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
            this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);

            // create a tile cache of suitable size
            this.tileCache = AndroidUtil.createTileCache(this, "mapcache",
                    mapView.getModel().displayModel.getTileSize(), 1f,
                    this.mapView.getModel().frameBufferModel.getOverdrawFactor());
        }
        catch (Exception ex)
        {
            Log.i("INFO", "Error showing map");
        }


        //Refreshing bus position
        handler.postDelayed(runnable, 5000);



        //Chart Zone
        extras = getIntent().getExtras();
        if (extras != null) {

            conn = new SQLServerConnection();
            barChart = (BarChart) findViewById(R.id.chart);

            barChart.setDrawBarShadow(false);
            barChart.setDrawValueAboveBar(true);
            barChart.setMaxVisibleValueCount(60);

            // scaling can now only be done on x- and y-axis separately
            barChart.setPinchZoom(false);
            barChart.setDrawGridBackground(false);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setSpaceBetweenLabels(2);

            YAxis leftAxis = barChart.getAxisLeft();
            //leftAxis.setTypeface(mTf);
            leftAxis.setLabelCount(8, false);
            //leftAxis.setValueFormatter(custom);
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setSpaceTop(15f);
            leftAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

            YAxis rightAxis = barChart.getAxisRight();
            rightAxis.setDrawGridLines(false);
            rightAxis.setLabelCount(8, false);
            rightAxis.setSpaceTop(15f);
            rightAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

            Legend l = barChart.getLegend();
            l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
            l.setForm(Legend.LegendForm.CIRCLE);
            l.setFormSize(9f);
            l.setTextSize(11f);
            l.setXEntrySpace(4f);

            barChart.setData(GetHourChartData(conn.GetGeneralStats(getCurrentDate(), extras.getString("CLIENT_CODE"), extras.getString("PROMOTION_CODE"))));
            barChart.animateY(2000);

            Button btnTotalStats=(Button)findViewById(R.id.btnTotalStats);
            Button btnHourStats=(Button)findViewById(R.id.btnHourStats);
            txtReportName=(TextView) findViewById(R.id.txtReportName);

            btnTotalStats.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    txtReportName.setText("Reporte de campaña");
                    barChart.setData(GetTotalChartData(conn.GetGeneralStats(getCurrentDate(), extras.getString("CLIENT_CODE"), extras.getString("PROMOTION_CODE"))));
                    barChart.animateY(2000);
                }
            });

            btnHourStats.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    txtReportName.setText("Reporte de horario");
                    barChart.setData(GetHourChartData(conn.GetGeneralStats(getCurrentDate(), extras.getString("CLIENT_CODE"), extras.getString("PROMOTION_CODE"))));
                    barChart.animateY(2000);
                }
            });

        }


    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
      /* do what you need to do */
            //Get las bus position
            ResultSet rs=conn.GetLastBusRoutePosition("ABX25");
            float latitude=10.93436F;
            float longitude=-74.79205F;

            try {
                while (rs.next())
                {
                    latitude=rs.getFloat("latitude");
                    longitude=rs.getFloat("longitude");
                }


                //Marker del bus en el mapa
                org.mapsforge.core.graphics.Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.drawable.bus_24));

                if (busMarker == null) {
                    busMarker = new Marker(new LatLong(latitude, longitude), bitmap, 0, 0);
                    mapView.getLayerManager().getLayers().add(busMarker);
                }

                //Si se encuentra el marcador entonces este se mueve
                busMarker.setLatLong(new LatLong(latitude, longitude));

                mapView.getModel().mapViewPosition.setCenter(new LatLong(latitude, longitude));

                //Lineas agregadas para repitar el mapa
                mapView.postInvalidate();

                tileRendererLayer.requestRedraw();
                mapView.repaint();

          /* and here comes the "trick" */
                handler.postDelayed(this, 5000);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    };

    private void reset() {
        numberOfLines = 1;

        hasAxes = true;
        hasAxesNames = true;
        hasLines = true;
        hasPoints = true;
        hasLabels = false;
        isCubic = false;

    }

    private void SetChartData2(ResultSet resultSet)
    {

        int i=0;

        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        List<Line> lines = new ArrayList<Line>();
        List<PointValue> pointsTotalTicketsToday = new ArrayList<PointValue>();
        List<PointValue> pointsTotalPlaysToday = new ArrayList<PointValue>();
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int firstValue=-1;

        //Toco hacer unos trucos para este chart mas que todo por los textos de que salian corridos debido a que el chart
        //si le agregas valores desde el 5 para la X entonces pinta la serie pero no desde el principio del cruce de los vertices x y Y

        if (resultSet!=null)
        {
            try {
                while (resultSet.next())
                {

                    if (firstValue==-1)
                    {
                        firstValue= resultSet.getInt("OnHour");
                    }

                    values = new ArrayList<SubcolumnValue>();

                    values.add(new SubcolumnValue(resultSet.getInt("TotalTicketsAllTime"), ChartUtils.COLOR_BLUE));
                    values.add(new SubcolumnValue( resultSet.getInt("TotalPlaysAllTime"), ChartUtils.COLOR_GREEN));

                    pointsTotalTicketsToday.add(new PointValue(i,resultSet.getInt("TotalTicketsToday") ));
                    pointsTotalPlaysToday.add(new PointValue(i,resultSet.getInt("TotalPlaysToday") ));

                    columns.add(new Column(values));
                    columns.get(i).setHasLabels(true);
                    axisValues.add(new AxisValue(i,String.valueOf(firstValue+i).toCharArray()));

                    i++;
                }



                Axis axisX = new Axis(axisValues);



                Line line = new Line(pointsTotalTicketsToday);
                line.setColor(Color.rgb(27, 94, 32));
                line.setCubic(true);
                line.setHasLabels(true);
                line.setHasLines(true);
                line.setHasPoints(true);
                lines.add(line);

                Line line2 = new Line(pointsTotalPlaysToday);
                line2.setColor(Color.rgb(13, 71, 161));
                line2.setCubic(true);
                line2.setHasLabels(true);
                line2.setHasLines(true);
                line2.setHasPoints(true);
                lines.add(line2);


                ColumnChartData columnChartData = new ColumnChartData(columns);
                LineChartData lineChartData = new LineChartData(lines);
                ComboLineColumnChartData data = new ComboLineColumnChartData(columnChartData, lineChartData);

                if (hasAxes) {
                    //Axis axisX = new Axis();
                    Axis axisY = new Axis().setHasLines(true);
                    if (hasAxesNames) {
                        axisX.setName("Axis X");
                        axisY.setName("Axis Y");
                    }
                    data.setAxisXBottom(axisX);
                    data.setAxisYLeft(axisY);
                } else {
                    data.setAxisXBottom(null);
                    data.setAxisYLeft(null);
                }

                chart.setComboLineColumnChartData( data);

               } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void SetChartData(ResultSet resultSet)
    {
/*
        int i=0;
        BarGraphSeries<DataPoint> serieTotalPlaysAllTime = new BarGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> serieTotalPlaysToday = new LineGraphSeries<DataPoint>();
        BarGraphSeries<DataPoint> serietotalTicketsAllTime = new BarGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> serietotalTicketsToday = new LineGraphSeries<DataPoint>();

        if (resultSet!=null)
        {
            try {
                while (resultSet.next())
                {
                    i++;
                    serieTotalPlaysAllTime.appendData(new DataPoint(resultSet.getInt("OnHour"),resultSet.getInt("TotalPlaysAllTime")), true,i);
                    serieTotalPlaysToday.appendData(new DataPoint(resultSet.getInt("OnHour"),resultSet.getInt("TotalPlaysToday")), true,i);
                    serietotalTicketsAllTime.appendData(new DataPoint(resultSet.getInt("OnHour"), resultSet.getInt("TotalTicketsAllTime")), true,i);
                    serietotalTicketsToday.appendData(new DataPoint(resultSet.getInt("OnHour"),resultSet.getInt("TotalTicketsToday")), true,i);



                }

                serietotalTicketsAllTime.setColor(Color.rgb(139, 195, 74));
                serietotalTicketsAllTime.setTitle("Entradas totales");
                serietotalTicketsAllTime.setSpacing(10);
                serietotalTicketsAllTime.setDrawValuesOnTop(true);
                serietotalTicketsAllTime.setValuesOnTopColor(Color.rgb(139, 195, 74));

                serieTotalPlaysAllTime.setColor(Color.rgb(3, 169, 244));
                serieTotalPlaysAllTime.setTitle("Reproducciones totales");
                serieTotalPlaysAllTime.setSpacing(10);
                serieTotalPlaysAllTime.setDrawValuesOnTop(true);
                serieTotalPlaysAllTime.setValuesOnTopColor(Color.rgb(3, 169, 244));


                serieTotalPlaysToday.setColor(Color.rgb(13, 71, 161));
                serieTotalPlaysToday.setTitle("Reproducciones hoy");
                serieTotalPlaysToday.setDrawDataPoints(true);

                serietotalTicketsToday.setColor(Color.rgb(27, 94, 32));
                serietotalTicketsToday.setTitle("Entradas hoy");
                serieTotalPlaysToday.setDrawDataPoints(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        graph.addSeries(serieTotalPlaysAllTime);
        graph.addSeries(serieTotalPlaysToday);
        graph.addSeries(serietotalTicketsAllTime);
        graph.addSeries(serietotalTicketsToday);


        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);
        */
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.mapView.getModel().mapViewPosition.setCenter(new LatLong(10.93436111111111, -74.79205));
        this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 16);

        // tile renderer layer using internal render theme
        MapDataStore mapDataStore = new MapFile(getMapFile());

        this.tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        //Lineas agregadas para repitar el mapa
        mapView.postInvalidate();

        tileRendererLayer.requestRedraw();
        mapView.repaint();
    }

    /**
     * Obtiene el mapa de la ciudad. Si es la primera vez que se llama este metodo obtiene el archivo y lo guarda en la sd,
     * sino obtiene directamente el mapa desde la sdcard
     * @return Archivo con el mapa en formato binario
     */
    private File getMapFile() {

        //TODO: Este forma de operar debe cambiar. El incluir el mapa en los recursos del apk le agrega un peso innecesario para la descarga
        //y la instalacion. A mayor peso del archivo en la Play Store, va descendiendo la preferncia del usuario por descargarlo.
        //La idea es que el archivo de mapa se descargue desde la aplicacion

        String filePath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File targetFile=new File(filePath + "/barranquilla.map");

        if(targetFile.exists())
        {
            return targetFile;
        }
        else
        {
            try {
                InputStream fileIS = getResources().openRawResource(R.raw.barranquilla);

                //InputStream initialStream = new FileInputStream(new File("src/main/resources/sample.txt"));
                byte[] buffer = new byte[fileIS.available()];
                fileIS.read(buffer);

                targetFile.createNewFile();

                OutputStream outStream = new FileOutputStream(targetFile);
                outStream.write(buffer);

                outStream.close();

                return targetFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return targetFile;
    }

    private CombinedData GetChartData(ResultSet resultSet)
    {
        int i=0;

        ArrayList<String> xAxisValues=new ArrayList<>();
        ArrayList<BarEntry> totalPlaysAllTime = new ArrayList<>();
        ArrayList<Entry> totalPlaysToday = new ArrayList<>();
        ArrayList<BarEntry> totalTicketsAllTime = new ArrayList<>();
        ArrayList<Entry> totalTicketsToday = new ArrayList<>();
        ArrayList<BarEntry> totalPromos= new ArrayList<>();

        BarData barData=new BarData();
        LineData lineData=new LineData();
        CombinedData combinedData=null;

        if (resultSet!=null)
        {
            try {
                while (resultSet.next())
                {
                    xAxisValues.add(String.valueOf(resultSet.getInt("OnHour")));
                    totalPlaysAllTime.add(new BarEntry(resultSet.getInt("TotalPlaysAllTime"), i ));
                    //new BarEntry()
                    totalPlaysToday.add(new Entry(resultSet.getInt("TotalPlaysToday"), i ));
                    totalTicketsAllTime.add(new BarEntry(resultSet.getInt("TotalTicketsAllTime"), i ));
                    totalTicketsToday.add(new Entry(resultSet.getInt("TotalTicketsToday"), i ));
                    totalPromos.add(new BarEntry(resultSet.getInt("TotalPromos"), i ));

                    i++;
                }

                combinedData=new CombinedData(xAxisValues);

                BarDataSet barDataSetTotalPlaysAllTime = new BarDataSet(totalPlaysAllTime, "Reproducciones totales");
                barDataSetTotalPlaysAllTime.setColor(Color.rgb(3, 169, 244));
                //barDataSetTotalPlaysAllTime.setBarSpacePercent(80f);

                LineDataSet lineDataSetTotalPlaysToday = new LineDataSet(totalPlaysToday, "Reproducciones hoy");
                lineDataSetTotalPlaysToday.setColor(Color.rgb(13, 71, 161));
                lineDataSetTotalPlaysToday.setDrawCubic(true);

                BarDataSet barDataSetTotalTicketsAllTime = new BarDataSet(totalTicketsAllTime, "Entradas totales");
                barDataSetTotalTicketsAllTime.setColor(Color.rgb(139, 195, 74));
                //barDataSetTotalTicketsAllTime.setBarSpacePercent(80f);

                LineDataSet lineDataSetTotalTicketsToday = new LineDataSet(totalTicketsToday, "Entradas hoy");
                lineDataSetTotalTicketsToday.setColor(Color.rgb(27,94,32));
                lineDataSetTotalTicketsToday.setDrawCubic(true);

                BarDataSet barDataSetTotalPromos = new BarDataSet(totalPromos, "Promociones adquiridas");
                barDataSetTotalPromos.setColor(Color.rgb(255, 87, 34));
                //barDataSetTotalPromos.setBarSpacePercent(4f);

                //barData.setGroupSpace(5f);
                barData.addDataSet(barDataSetTotalPlaysAllTime);
                barData.addDataSet(barDataSetTotalTicketsAllTime);
                //barData.addDataSet(barDataSetTotalPromos);


                lineData.addDataSet(lineDataSetTotalPlaysToday);
                lineData.addDataSet(lineDataSetTotalTicketsToday);



                /*
                BarData barDataTotalPlaysAllTime = new BarData(xAxisValues,barDataSetTotalPlaysAllTime);
                LineData lineDataTotalPlaysToday = new LineData(xAxisValues,lineDataSetTotalPlaysToday);
                BarData barDataTotalTicketsAllTime = new BarData(xAxisValues,barDataSetTotalTicketsAllTime);
                LineData lineDataTotalTicketsToday = new LineData(xAxisValues,lineDataSetTotalTicketsToday);
                BarData barDataTotalPromos = new BarData(xAxisValues,barDataSetTotalPromos);
*/


                //combinedData.setData(barDataTotalPlaysAllTime);
                //combinedData.setData(lineDataTotalPlaysToday);
                //combinedData.setData(barDataTotalTicketsAllTime);
                //combinedData.setData(lineDataTotalTicketsToday);
                //combinedData.setData(barDataTotalPromos);

                combinedData.setData(barData);
                combinedData.setData(lineData);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return combinedData;
    }


    private BarData GetTotalChartData(ResultSet resultSet)
    {
        int i=0;

        ArrayList<String> xAxisValues=new ArrayList<>();
        ArrayList<BarEntry> totalPlaysAllTime = new ArrayList<>();
        ArrayList<BarEntry> totalTicketsAllTime = new ArrayList<>();
        ArrayList<BarEntry> totalPromos= new ArrayList<>();

        if (resultSet!=null)
        {
            try {
                while (resultSet.next())
                {
                    xAxisValues.add(String.valueOf(resultSet.getInt("OnHour")));
                    totalPlaysAllTime.add(new BarEntry(resultSet.getInt("TotalPlaysAllTime"), i ));
                    totalTicketsAllTime.add(new BarEntry(resultSet.getInt("TotalTicketsAllTime"), i ));
                    totalPromos.add(new BarEntry(resultSet.getInt("TotalPromos"), i ));

                    i++;
                }

                BarData barData=new BarData(xAxisValues);

                BarDataSet barDataSetTotalPlaysAllTime = new BarDataSet(totalPlaysAllTime, "Reproducciones campaña");
                barDataSetTotalPlaysAllTime.setColor(Color.rgb(3, 169, 244));
                //barDataSetTotalPlaysAllTime.setBarSpacePercent(80f);

                BarDataSet barDataSetTotalTicketsAllTime = new BarDataSet(totalTicketsAllTime, "Clientes potenciales");
                barDataSetTotalTicketsAllTime.setColor(Color.rgb(139, 195, 74));
                //barDataSetTotalTicketsAllTime.setBarSpacePercent(80f);


                barData.addDataSet(barDataSetTotalPlaysAllTime);
                barData.addDataSet(barDataSetTotalTicketsAllTime);


                return barData;

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private BarData GetHourChartData(ResultSet resultSet)
    {
        int i=0;

        ArrayList<String> xAxisValues=new ArrayList<>();
        ArrayList<BarEntry> totalPlaysToday = new ArrayList<>();
        ArrayList<BarEntry> totalTicketsToday = new ArrayList<>();
        ArrayList<BarEntry> totalPromos= new ArrayList<>();


        //LineData lineData=new LineData();
        //CombinedData combinedData=null;

        if (resultSet!=null)
        {
            try {
                while (resultSet.next())
                {
                    xAxisValues.add(String.valueOf(resultSet.getInt("OnHour")));
                    totalPlaysToday.add(new BarEntry(resultSet.getInt("TotalPlaysToday"), i ));
                    totalTicketsToday.add(new BarEntry(resultSet.getInt("TotalTicketsToday"), i ));
                    totalPromos.add(new BarEntry(resultSet.getInt("TotalPromos"), i ));

                    i++;
                }

                //combinedData=new CombinedData(xAxisValues);
                BarData barData=new BarData(xAxisValues);

                BarDataSet barDataSetTotalPlaysToday = new BarDataSet(totalPlaysToday, "Número de reproducciones");
                barDataSetTotalPlaysToday.setColor(Color.rgb(13, 71, 161));

                BarDataSet barDataSetTotalTicketsToday = new BarDataSet(totalTicketsToday, "Clientes potenciales dia");
                barDataSetTotalTicketsToday.setColor(Color.rgb(27, 94, 32));


                BarDataSet barDataSetTotalPromos = new BarDataSet(totalPromos, "Promociones");
                barDataSetTotalPromos.setColor(Color.rgb(255, 87, 34));


                barData.addDataSet(barDataSetTotalPlaysToday);
                barData.addDataSet(barDataSetTotalTicketsToday);
                barData.addDataSet(barDataSetTotalPromos);

                return barData;

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static java.sql.Timestamp getCurrentDate() {

        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();

        return new java.sql.Timestamp(now.getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stats, menu);
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
