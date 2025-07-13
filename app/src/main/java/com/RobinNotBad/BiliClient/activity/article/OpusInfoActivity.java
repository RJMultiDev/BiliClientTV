package com.RobinNotBad.BiliClient.activity.article;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.activity.reply.ReplyFragment;
import com.RobinNotBad.BiliClient.adapter.viewpager.ViewPagerFragmentAdapter;
import com.RobinNotBad.BiliClient.event.ReplyEvent;
import com.RobinNotBad.BiliClient.util.AnimationUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class OpusInfoActivity extends BaseActivity {
    private long oid;

    private ReplyFragment replyFragment;
    private long seek_reply;

    private ImageView loadingView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_viewpager);
        Intent intent = getIntent();
        oid = intent.getLongExtra("id", 114514);
        this.seek_reply = getIntent().getLongExtra("seekReply", -1);

        setPageName("文章详情");
        loadingView = findViewById(R.id.loading);

        //TutorialHelper.showTutorialList(this, R.array.tutorial_article, 7);

        ViewPager viewPager = findViewById(R.id.viewPager);

        List<Fragment> fragmentList = new ArrayList<>();
        OpusInfoFragment oiFragment = OpusInfoFragment.newInstance(oid);
        fragmentList.add(oiFragment);
        ViewPagerFragmentAdapter vpfAdapter = new ViewPagerFragmentAdapter(getSupportFragmentManager(), fragmentList);
        viewPager.setAdapter(vpfAdapter);

        AnimationUtils.crossFade(loadingView, oiFragment.getView());
        /*
        TerminalContext.getInstance().getArticleInfoByCvId(cvid)
            .observe(this, (result) -> result.onSuccess((articleInfo)-> {
                List<Fragment> fragmentList = new ArrayList<>();
                ArticleInfoFragment articleInfoFragment = ArticleInfoFragment.newInstance(cvid);
                fragmentList.add(articleInfoFragment);
                replyFragment = ReplyFragment.newInstance(cvid, ReplyApi.REPLY_TYPE_ARTICLE, articleInfo.stats.reply, seek_reply, articleInfo.upInfo.mid);
                replyFragment.setManager(articleInfo.upInfo);
                fragmentList.add(replyFragment);
                ViewPagerFragmentAdapter vpfAdapter = new ViewPagerFragmentAdapter(getSupportFragmentManager(), fragmentList);
                viewPager.setAdapter(vpfAdapter);
                View view;
                if ((view = articleInfoFragment.getView()) != null)
                    view.setVisibility(View.GONE);
                if (seek_reply != -1) viewPager.setCurrentItem(1);
                articleInfoFragment.setOnFinishLoad(() -> AnimationUtils.crossFade(loadingView, articleInfoFragment.getView()));
                TutorialHelper.showPagerTutorial(this,2);
            }).onFailure((error) -> {
                loadingView.setImageResource(R.mipmap.loading_2233_error);
                MsgUtil.err(error);
            }));

         */
    }

    @Override
    protected boolean eventBusEnabled() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true, priority = 1)
    public void onEvent(ReplyEvent event) {
        //replyFragment.notifyReplyInserted(event);
    }

    @Override
    protected void onDestroy() {
        //TerminalContext.getInstance().leaveDetailPage();
        super.onDestroy();
    }
}
