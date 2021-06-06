package cc.kinami.audiotool;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import cc.kinami.audiotool.adapter.BasicFragmentAdapter;
import cc.kinami.audiotool.fragment.BeepbeepClientFragment;
import cc.kinami.audiotool.fragment.RecordFragment;

public class TabTestActivity extends AppCompatActivity {
    private final String[] titles = {"Record", "Beepbeep"};
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_test);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new RecordFragment());
        fragmentList.add(new BeepbeepClientFragment());
        BasicFragmentAdapter adapter = new BasicFragmentAdapter(getSupportFragmentManager(), fragmentList, titles);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

    }
}
