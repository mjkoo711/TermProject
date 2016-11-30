package my.kmucs.com.koo_timer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Button countUp;

    Fragment1 fragment1;
    Fragment2 fragment2;
    Fragment3 fragment3;
    TabLayout tabs;

    private BackPressCloseHandler backPressCloseHandler;

    //센서관련객체
    SensorManager sensorManager;
    Sensor sensor;

    TextView mEllapse;
    TextView mSplit;

    //스톱워치의 상태를 위한 상수
    int mStatus = 0;
    long mBaseTime, mPauseTime;
    long now;

    //스톱워치를 위해 핸들러를 만든다.
    Handler mTimer = new Handler(){
        //핸들러는 기본적으로 handleMessage에서 처리한다.
        public void handleMessage(android.os.Message msg){
            //텍스트뷰를 수정해준다.
            mEllapse.setText(getEllapse());
            //메시지를 다시보낸다
            mTimer.sendEmptyMessage(0);//0은 메시지를 구분하기 위한것
        };
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //시스템서비스로부터 SensorManager 객체를 얻는다.
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //Sensormanager를 이용해서 근접 센서 객체를 얻는다.
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        //뒤로가기 두번누르게 하는 객체
        backPressCloseHandler = new BackPressCloseHandler(this);

        fragment1 = new Fragment1();
        fragment2 = new Fragment2();
        fragment3 = new Fragment3();

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1).commit();

        tabs = (TabLayout)findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText("달력"));
        tabs.addTab(tabs.newTab().setText("통계"));
        tabs.addTab(tabs.newTab().setText("메모"));

        mEllapse = (TextView)findViewById(R.id.ellapse);
        mSplit = (TextView)findViewById(R.id.split);
        countUp = (Button)findViewById(R.id.countUp);

        countUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (countUp.getText().toString()){
                    case "START":
                        now = mPauseTime= mBaseTime = SystemClock.elapsedRealtime();
                        buttonClickTimeStart();
                        countUp.setText("RESET");
                        Toast.makeText(getApplicationContext(),"휴대폰 화면을 엎어놓으면 스톱워치가 시작합니다.\n휴대폰 화면을 다시 돌리면 스톱워치가 일시정지 합니다.",Toast.LENGTH_LONG).show();
                        break;

                    case "RESET":
                        buttonClickTimeStop();
                        mEllapse.setText("00:00:00");
                        countUp.setText("START");
                        break;
                }
            }
        });

        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("MainActivity", "선택된 탭 : " + position);

                Fragment selected = null;
                if(position == 0){
                    selected = fragment1;
                }
                else if(position == 1){
                    selected = fragment2;
                }
                else if(position == 2){
                    selected = fragment3;
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.container, selected).commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("MainActivity", "선택된 탭 : " + position);

                Fragment selected = null;
                if(position == 0){
                    selected = fragment1;
                }
                else if(position == 1){
                    selected = fragment2;
                }
                else if(position == 2){
                    selected = fragment3;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selected).commit();
            }
        });
    }



    public void buttonClickTimeStart(){
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void buttonClickTimeStop(){
        //센서 값이 필요하지 않는 시점에 리스너를 해제해준다.
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            //몇몇 기기의 경우 accuracy가 SENSOR_STATUS_UNRELIABLE 값을
            //가져서 측정값을 사용하지 못하는 경우가 있기 때문에 임의로 return;을 막는다
            //return ;
        }

        //센서값을 측정한 센서의 종류가 근접 센서인 경우
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -0.01 && event.values[0] <= 0.01) {
                //near

                Toast.makeText(getApplicationContext(), "near", Toast.LENGTH_SHORT).show();
                //현재값 가져옴
                now = SystemClock.elapsedRealtime();
                //베이스타임 = 베이스타임 + (now - mPauseTime)
                //잠깐 스톱워치를 멈췄다가 다시 시작하면 기준점이 변하게 되므로
                mBaseTime += (now - mPauseTime);
                mTimer.sendEmptyMessage(0);

            } else {

                mTimer.removeMessages(0);
                mPauseTime = SystemClock.elapsedRealtime();
                mStatus = 3;
                //far
                Toast.makeText(getApplicationContext(), "far", Toast.LENGTH_SHORT).show();

            }
        }
    }
    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    String getEllapse(){
        long now = SystemClock.elapsedRealtime();
        long ell = now - mBaseTime; //현재시간과 지난 시간을 뺴서 ell값을 구하고
        //아래에서 포맷을 예쁘게 바꾼다음 리턴해준다.
        String sEll = String.format("%02d:%02d:%02d", ell/1000/60, (ell/1000)%60, (ell % 1000)/10);
        return sEll;
    }

    //정확도 변경시 호출되는 메소드. 센서의 경우 거의호출되지 않는다.
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}