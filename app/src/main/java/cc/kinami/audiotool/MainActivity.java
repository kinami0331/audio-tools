

package cc.kinami.audiotool;


import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import cc.kinami.audiotool.adapter.BasicFragmentAdapter;
import cc.kinami.audiotool.fragment.BeepControlFragment;
import cc.kinami.audiotool.fragment.WsClientFragment;
import cc.kinami.audiotool.fragment.ExperimentType2ControlFragment;
import cc.kinami.audiotool.fragment.PlayAndRecordFragment;

public class MainActivity extends AppCompatActivity {


    private final Rationale<java.util.List<java.lang.String>> rationale = (context, data, executor) -> {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("提示")
                .setMessage("录制声音需要录音和麦克风权限")
                .setPositiveButton("允许", (dialog, which) -> executor.execute())
                .setNegativeButton("拒绝", (dialog, which) -> executor.execute())
                .create();
        alertDialog.show();
    };

    private final String[] titles = {"WS", "EXP 1 Ctrl", "EXP 2 Ctrl", "P&R"};
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new WsClientFragment());
        fragmentList.add(new BeepControlFragment());
        fragmentList.add(new ExperimentType2ControlFragment());
        fragmentList.add(new PlayAndRecordFragment());
        BasicFragmentAdapter adapter = new BasicFragmentAdapter(getSupportFragmentManager(), fragmentList, titles);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    protected void onStart() {
        super.onStart();

        /*
         * 准备录音 录音之前 先判断是否有相关权限
         */
        getPermission();
    }

    private void getPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.MICROPHONE, Permission.Group.STORAGE)
                .rationale(rationale)
                .onDenied(permissions -> {
                    if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions))
                        System.out.println("哈哈哈");
                })
                .start();
    }

}