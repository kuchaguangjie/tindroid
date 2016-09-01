package co.tinode.tindroid;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import java.util.ArrayList;

import co.tinode.tinodesdk.MeTopic;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.Description;
import co.tinode.tinodesdk.model.Invitation;
import co.tinode.tinodesdk.model.MsgServerData;
import co.tinode.tinodesdk.model.MsgServerInfo;
import co.tinode.tinodesdk.model.MsgServerMeta;
import co.tinode.tinodesdk.model.Subscription;

/**
 * Display user's list of contacts
 */

public class ContactsActivity extends AppCompatActivity {

    private static final String TAG = "ContactsActivity";

    protected ChatListAdapter mContactsAdapter;
    protected ArrayList<String> mContactIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contacts);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabsContacts);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.tabPager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mContactIndex = new ArrayList<>();
        mContactsAdapter = new ChatListAdapter(this, mContactIndex);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        MeTopic<VCard,String,String> me = InmemoryCache.getTinode().getMeTopic();
        if (me == null) {
            me = new MeTopic<>(InmemoryCache.getTinode(),
                    new Topic.Listener<VCard, String, Invitation<String>>() {

                        @Override
                        public void onData(MsgServerData<Invitation<String>> data) {
                            // TODO(gene): handle a chat invitation
                            Log.d(TAG, "Contacts got an invitation to topic " + data.content.topic);
                        }

                        @Override
                        public void onContactUpdate(final String what, final Subscription<VCard, String> sub) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mContactsAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void onInfo(MsgServerInfo info) {
                            Log.d(TAG, "Contacts got onInfo update '" + info.what + "'");
                        }

                        @Override
                        public void onMeta(MsgServerMeta<VCard, String> meta) {

                        }

                        @Override
                        public void onMetaSub(Subscription<VCard, String> sub) {
                            sub.pub.constructBitmap();
                            mContactIndex.add(sub.topic);
                        }

                        @Override
                        public void onMetaDesc(final Description<VCard, String> desc) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (desc.pub != null) {
                                        InmemoryCache.setupToolbar(ContactsActivity.this, toolbar, desc.pub, Topic.TopicType.ME);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onSubsUpdated() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mContactsAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
            // Public, Private, Info in Invite<Info>
            me.setTypes(VCard.class, String.class, String.class);
            InmemoryCache.getTinode().registerTopic(me);
            try {
                me.subscribe();
            } catch (Exception err) {
                Log.i(TAG, "connection failed :( " + err.getMessage());
                Toast.makeText(getApplicationContext(),
                        "Failed to login", Toast.LENGTH_LONG).show();
            }
        } else {
            mContactIndex.clear();
            for (Subscription<VCard, String> s : me.getSubscriptions()) {
                mContactIndex.add(s.topic);
            }
            mContactsAdapter.notifyDataSetChanged();
        }
    }

    protected ChatListAdapter getContactsAdapter() {
        return mContactsAdapter;
    }

    protected Subscription<VCard,String> getContactByPos(int pos) {
        MeTopic<VCard,String,String> me = InmemoryCache.getTinode().getMeTopic();
        return me.getSubscription(mContactIndex.get(pos));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat_list, menu);
        return true;
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        int mNumOfTabs;

        public PagerAdapter(FragmentManager fm, int numTabs) {
            super(fm);
            mNumOfTabs = numTabs;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new ChatListFragment();
                case 1:
                    return new ContactsFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }
    }
}