package com.tgv.tgv_delivery;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TourneeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournee);
        // on récupère un accès sur le ViewPager défini dans la vue
        ViewPager2 pager = findViewById(R.id.activity_main_viewpager);
        TabLayout gestionnaireOnglet = findViewById(R.id.tab_layout);

        /*
         * on associe au ViewPager un adaptateur
         */
        pager.setAdapter(new TourneeFragmentAdapter(this));


        /*
         * On regroupe dans un tableau les intitulés des boutons d'onglet
         */
        String[] titreOnglet = {getString(R.string.onglet_colis),
                getString(R.string.onglet_carte), getString(R.string.onglet_boucle)};
        /*
         * On crée une instance de type TabLayoutMediator qui fera le lien entre
         * le gestionnaire de pagination et le gestionnaire des onglets
         * La méthode onConfigureTab permet de préciser quel intitulé de bouton
         * d'onglets correspond à tel ou tel onglet, selon la position de celui-ci
         * L'instance TabLayoutMediator est attachée à l'activité courante
         *
         */
        new TabLayoutMediator(gestionnaireOnglet, pager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override public void onConfigureTab(TabLayout.Tab tab,
                                                         int position) {
                        tab.setText(titreOnglet[position]);
                    }
                }).attach();

        gestionnaireOnglet.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    public void onTabSelected(TabLayout.Tab tab) {
                        /*
                         * On récupère le numéro du fragment courant
                         * La méthode getCurrrentItem renvoie le numéro (ou la position, conformément
                         * à ce qui a été fait dans la classe AdaptateurPage) du fragment
                         * actuellement affiché.
                         */
                        int numFragCourant = pager.getCurrentItem();
                        /*
                         * on réucpère le fragment courant (celui qui est affiché) à partir de
                         * son identifiant, grâce à la méthode findFragmentByTag
                         * Par convention, un identifiant de fragment est de la forme
                         * f suivi du numéro du fragment. Donc f0 ou f1, par exemple
                         */
                        Fragment courant =
                                getSupportFragmentManager().findFragmentByTag("f" +numFragCourant);
                    }
                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        // ne rien faire
                    }
                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        // ne rien faire
                    }
                }
        );
    }
}