package com.lecraftjay.newgrounds.nav_window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.lecraftjay.newgrounds.R;
import com.lecraftjay.newgrounds.classes.Var;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MovieActivity extends AppCompatActivity {

    int pos = 0;
    ArrayList<String> movieContent = new ArrayList<>();

    LinearLayout root;
    ScrollView scrollLayout;
    ProgressBar space;

    boolean einmal = false;
    int adCounter = 0;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);

        //-------------------------------------------------------------------

        root = findViewById(R.id.movieLayout);
        scrollLayout = findViewById(R.id.movieScroll);
        space = findViewById(R.id.movieSpace);

        //-------------------------------------------------------------------

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
        
        setNavigation();

        scrollLayout.getViewTreeObserver()
                .addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (scrollLayout.getChildAt(0).getBottom() <= (scrollLayout.getHeight() + scrollLayout.getScrollY())) {
                            if(einmal == false) {
                                space.setVisibility(View.VISIBLE);
                                getContent("https://www.newgrounds.com/movie/featured?offset=;;;pos;;;&amp;inner=1", true);
                                einmal = true;
                            }
                        } else {
                            einmal = false;
                        }
                    }
                });

        getContent("https://www.newgrounds.com/movies", false);
    }

    public void getContent(String url, boolean useAdvanced){
        if(useAdvanced){
            pos += 20;
        }

        String newLink = url.replace(";;;pos;;;", String.valueOf(pos));

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Document doc = (Document) Jsoup
                            .connect(newLink)
                            .userAgent(
                                    "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36").ignoreHttpErrors(true)
                            .timeout(5000).followRedirects(true).execute().parse();


                    Elements ele = doc.getElementsByClass("portalsubmission-cell");
                    for (Element l : ele) {

                        Elements link = l.select("a");
                        Elements title = l.select("img");
                        Elements imgLink = l.getElementsByClass("card-img");
                        Elements creator = l.select("span");

                        String s = "";
                        for(Element e : title){
                            s = e.attr("alt");
                        }

                        String s1 = "---";
                        for(Element e : imgLink){
                            s1 = e.attr("src");
                        }

                        String sLink = link.attr("abs:href");
                        String sTitle = s;
                        String sImgLink = s1;
                        String sCreator = creator.html();

                        if(sLink.contains("")) {
                            if(movieContent.contains(sLink)){

                            }else {
                                String toAdd = sLink + ";;;" + sTitle + ";;;" + sImgLink + ";;;" + sCreator;
                                movieContent.add(toAdd);
                            }
                        }

                    }

                }catch (SocketTimeoutException e){
                    e.printStackTrace();
                    //error = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });
        t.start();
    }

    @Override
    protected void onResume() {
        //start handler as activity become visible

        handler.postDelayed( runnable = new Runnable() {
            public void run() {
                update();

                handler.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }


    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    public String trim(String text, int index){
        if(text.length() > index){
            text = text.substring(0,index) + "...";
            return text;
        }
        return text;
    }

    public void update(){
        if(root.getChildCount() < movieContent.size() || Var.updateNow) {

            Var.updateNow = false;
            root.removeAllViews();
            space.setVisibility(View.INVISIBLE);
            for (int i = 0; i < movieContent.size(); i++) {

                View view = LayoutInflater.from(MovieActivity.this).inflate(R.layout.movie_card_layout, null);
                TextView cardTitle = view.findViewById(R.id.movieTitle);
                ImageView image = view.findViewById(R.id.movieImage);
                TextView user = view.findViewById(R.id.movieCreator);

                String title = movieContent.get(i);
                String[] splitter = title.split(";;;");

                try {
                    cardTitle.setTag(splitter[0]);
                    cardTitle.setText(trim(splitter[1], 25));
                    Picasso.get().load(splitter[2]).into(image);
                    user.setText(splitter[3]);
                }catch (ArrayIndexOutOfBoundsException e){
                    e.printStackTrace();
                }

                SharedPreferences sp = getApplicationContext().getSharedPreferences("Movie", 0);
                String getter = sp.getString("alreadySeen", "");

                if(getter.contains(splitter[0])){
                    cardTitle.setTextColor(ContextCompat.getColor(MovieActivity.this, R.color.audioSeen));
                }

                if(adCounter >= 3){
                    adCounter = 0;
                    AdView ad = new AdView(this);
                    ad.setAdUnitId("ca-app-pub-3904729559747077/1324825772");
                    ad.setAdSize(AdSize.BANNER);
                    root.addView(ad);
                    AdRequest request = new AdRequest.Builder().build();
                    ad.loadAd(request);
                }
                adCounter++;

                //cardText.setText(title);
                root.addView(view);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView title = view.findViewById(R.id.movieTitle);
                        Var.movieTitle = (String) title.getTag();
                        Var.movieOpenLink = (String) title.getTag();

                        SharedPreferences sp = getApplicationContext().getSharedPreferences("Movie", 0);
                        String getter = sp.getString("alreadySeen", "");

                        SharedPreferences liste = getApplicationContext().getSharedPreferences("Movie", 0);
                        SharedPreferences.Editor editor = liste.edit();
                        editor.putString("alreadySeen", getter + ";;;" + Var.movieOpenLink);
                        editor.apply();

                        title.setTextColor(ContextCompat.getColor(MovieActivity.this, R.color.audioSeen));
                        System.out.println("jason movie url: " + Var.movieOpenLink);

                        Uri uri = Uri.parse(Var.movieOpenLink); // missing 'http://' will cause crashed
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });
            }
            root.addView(space);
        }
    }






    public void setNavigation(){
        LinearLayout games = findViewById(R.id.games);
        LinearLayout movie = findViewById(R.id.movie);
        LinearLayout audio = findViewById(R.id.audio);
        LinearLayout art = findViewById(R.id.art);
        LinearLayout community = findViewById(R.id.profile);

        games.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MovieActivity.this, GamesActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MovieActivity.this, AudioActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        art.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MovieActivity.this, ArtActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        community.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MovieActivity.this, ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }
}