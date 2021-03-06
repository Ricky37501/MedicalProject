package com.medicalproject.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.myapplication.R;
import com.medicalproject.service.ListenService;
import com.medicalproject.util.DBUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * ListView医生适配器类
 * @author 汪文博
 */
public class DoctorAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private ArrayList<HashMap<String, Object>> list;

    public DoctorAdapter(Context context, ArrayList<HashMap<String, Object>> list){
        this.layoutInflater = LayoutInflater.from(context);
        this.list = list;
    }

    //数据源长度
    @Override
    public int getCount(){
        return list.size();
    }

    //每一行的绑定数据源
    @Override
    public Object getItem(int position){
        return list.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    //获取每一行的view
    @Override
    public View getView(final int position, View convertView, ViewGroup parent){
        ViewHolder holder;
        if(convertView == null){
            convertView = layoutInflater.inflate(R.layout.item, parent, false);
            holder = new ViewHolder();
            //得到各个控件的对象
            holder.ItemName = (TextView) convertView.findViewById(R.id.tv_doctor_name);
            holder.ItemIntro = (TextView) convertView.findViewById(R.id.tv_doctor_intro);
            holder.ItemBtn = (Button) convertView.findViewById(R.id.btn_doctor_item);
            convertView.setTag(holder);    //绑定ViewHolder对象
        }else{
            holder = (ViewHolder) convertView.getTag();    //取出ViewHolder对象
        }

        //设置TextView显示的内容
        holder.ItemName.setText(list.get(position).get("ItemName").toString());
        holder.ItemIntro.setText(list.get(position).get("ItemIntro").toString());

        //为TextView添加点击事件，显示详细信息
        holder.ItemName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage(list.get(position).get("ItemName").toString(), list.get(position).get("ItemIntro").toString());
            }
        });
        holder.ItemIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage(list.get(position).get("ItemName").toString(), list.get(position).get("ItemIntro").toString());
            }
        });

        //为Button添加点击事件
        holder.ItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //此处设置挂号功能
                //获取患者ID
                SharedPreferences sp = layoutInflater.getContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                final String userID = sp.getString("userID", "");

                //开启线程进行数据库操作
                new Thread(new Runnable() {
                    String userIDInner = userID;
                    @Override
                    public void run() {
                        Message message = handler.obtainMessage();
                        //获取当前挂号人数
                        int teamPeople = DBUtils.getCurrentPeople(list.get(position).get("ItemName").toString());
                        Calendar calendar = new GregorianCalendar();
                        if(calendar.get(Calendar.HOUR_OF_DAY) < 9){
                            message.obj = "未到上班时间，无法挂号";
                        }else if(calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE)+(teamPeople+1)*15 > 17*60){
                            message.obj = "排队人数过多，今日无法排到，请明天再挂号";
                        }else{
                            message.obj = "挂号成功";
                        }

                        DBUtils.addCallNumber(userID, list.get(position).get("ItemName").toString());
                        handler.sendMessage(message);
                    }
                }).start();
                for(int i=0; i<1000000; i++){}

                //开启Service监听
                //存储医生姓名
                SharedPreferences.Editor editDoctor = sp.edit();
                editDoctor.putString("doctorName", list.get(position).get("ItemName").toString());
                editDoctor.commit();

                Intent intent = new Intent(layoutInflater.getContext(), ListenService.class);
                layoutInflater.getContext().startService(intent);
            }
        });
        return convertView;
    }

    //存放控件
    private class ViewHolder{
        private TextView ItemName;
        private TextView ItemIntro;
        private Button ItemBtn;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String resp = (String) msg.obj;
            Toast.makeText(layoutInflater.getContext(), resp, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 显示医生详细信息
     * @param name 医生姓名
     * @param intro 医生介绍
     */
    private void showMessage(String name, String intro){
        AlertDialog.Builder builder = new AlertDialog.Builder(layoutInflater.getContext());
        builder.setTitle(name);
        builder.setMessage(intro);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setPositiveButton("返回", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }
}
