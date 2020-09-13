package adapto.com.menuc.Fragmentos.Forum;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import adapto.com.menuc.DevelopmentVariables;
import adapto.com.menuc.FirebaseDataAuth;
import adapto.com.menuc.FirebaseViewHolder;
import adapto.com.menuc.Fragmentos.Forum.Firebase.FirebaseForumPreviewDataAuth;
import adapto.com.menuc.Fragmentos.Forum.Firebase.FirebaseForumPreviewViewHolder;
import adapto.com.menuc.MainActivities.CriarPostagemForumActivity;
import adapto.com.menuc.MainActivities.DetalharPostagemForumActivity;
import adapto.com.menuc.R;

public class ForumFragment extends Fragment {

    private FirebaseRecyclerAdapter<FirebaseForumPreviewDataAuth, FirebaseForumPreviewViewHolder> forumPreviewAdapter;
    private Query dbForum = FirebaseDatabase.getInstance().getReference().child(DevelopmentVariables.FORUM.toString());
    private DatabaseReference dbRespostas = FirebaseDatabase.getInstance().getReference().child(DevelopmentVariables.RESPOSTAS.toString());
    private DatabaseReference dbRefAutores = FirebaseDatabase.getInstance().getReference().child("Autores");
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    RecyclerView recyclerViewForum;
    FloatingActionButton fabPostarForum;
    ArrayList<FirebaseForumPreviewDataAuth> forumPreviewArrayList;
    FirebaseRecyclerOptions<FirebaseForumPreviewDataAuth> forumPreviewoptions;
    private Set<String> listaAutoresDB;

    public ForumFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);
        return view;
    }

    private void listarAutores() {
        final Query checkQ = dbRefAutores.orderByChild("nomeIndicado");
        if(listaAutoresDB.isEmpty()) {
            checkQ.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        listaAutoresDB.add(postSnapshot.child("emailIndicado").getValue().toString());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }


            });
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        recyclerViewForum = getView().findViewById(R.id.lista_forum_preview);
        fabPostarForum = getView().findViewById(R.id.add_forum_fab);
        recyclerViewForum.setHasFixedSize(true);
        recyclerViewForum.setLayoutManager(new LinearLayoutManager(getContext()));
        forumPreviewArrayList = new ArrayList<>();
        dbForum.keepSynced(true);
        listaAutoresDB = new HashSet<>();
        listarAutores();
        dbRefAutores.keepSynced(true);
        forumPreviewoptions = new FirebaseRecyclerOptions.Builder<FirebaseForumPreviewDataAuth>().
                setQuery(dbForum.orderByKey(), FirebaseForumPreviewDataAuth.class)
                .setLifecycleOwner(this)
                .build();
        fabPostarForum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CriarPostagemForumActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        Set<String> tempList = preferences.getStringSet("LISTA_AUTORES_TELAINICIAL", null);
        if(tempList != null)
            listaAutoresDB.addAll(tempList);
    }

    @Override
    public void onStart() {
        super.onStart();
        listarAutores();
        createAdapter();
        recyclerViewForum.setAdapter(forumPreviewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void createAdapter() {
        forumPreviewAdapter = new FirebaseRecyclerAdapter<FirebaseForumPreviewDataAuth, FirebaseForumPreviewViewHolder>(forumPreviewoptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FirebaseForumPreviewViewHolder holder, final int position, @NonNull final FirebaseForumPreviewDataAuth model) {
                holder.forum_preview_autor.setText("Por: " + formatarNomeAutor(model.getfAutor()));
                holder.forum_preview_qtdRespostas.setText(model.getfQtdComentarios() + "");
                holder.forum_preview_data.setText(model.getfDataPostagem());
                holder.forum_preview_titulo.setText(model.getfTitulo());
                if(checkifAutor(model.getfEmailAutor()))
                    holder.forum_preview_cargo.setText("MODERADOR");

                if(checkifAutor(mAuth.getCurrentUser().getEmail())) {
                    holder.btn_deletar_postagem.setVisibility(View.VISIBLE);
                    holder.btn_deletar_postagem.setClickable(true);
                }else{
                    holder.btn_deletar_postagem.setClickable(false);
                }


                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), DetalharPostagemForumActivity.class);
                        intent.putExtra("postagemKey", model.getKey());
                        startActivity(intent);
                    }
                });

                deny(holder, model);

            }

            @NonNull
            @Override
            public FirebaseForumPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new FirebaseForumPreviewViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.cardview_preview_forum, parent,false));
            }
        };
    }

    private boolean checkifAutor(String getfEmailAutor) {
        boolean check = false;
        if(getfEmailAutor != null) {
            for (String name : listaAutoresDB) {
                if (name.equals(getfEmailAutor.trim()))
                    check = true;
            }
        }
        return check;
    }

    private String formatarNomeAutor(String getfAutor) {
        String[] nomes = getfAutor.split(" ");
        String nomeFormatado = null;
        return nomeFormatado = (nomes.length > 1) ?  nomes[0] + " "+ nomes[1] :  nomes[0];
    }

    void deny(@NonNull final FirebaseForumPreviewViewHolder holder, @NonNull final FirebaseForumPreviewDataAuth model){
        holder.btn_deletar_postagem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialogExcluirPostagem(model, v, holder);
            }
        });
    }

    public void showAlertDialogExcluirPostagem(@NonNull final FirebaseForumPreviewDataAuth model, View v, final FirebaseForumPreviewViewHolder holder) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("ATENÇÃO!");
        builder.setMessage("Você tem certeza que deseja excluir esta postagem?");
        // add a button
        builder.setPositiveButton("Prosseguir",new DialogInterface
                .OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog,
                                int which)
            {
                final Query excludePostagem =  dbForum.orderByChild("key").equalTo(model.getKey());
                excludePostagem.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot dataSnapshot1: dataSnapshot.getChildren()){
                            dataSnapshot1.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Log.e(TAG, "onCancelled", databaseError.toException());

                    }
                });

                final Query excludeRespostas = dbRespostas.orderByChild("postagemKey").equalTo(model.getKey());
                excludeRespostas.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot dS1: dataSnapshot.getChildren()){
                            dS1.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        });
        builder.setNegativeButton("Voltar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


}
