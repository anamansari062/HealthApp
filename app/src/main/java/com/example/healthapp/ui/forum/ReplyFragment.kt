package com.example.healthapp.ui.forum

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gdsc_hackathon.extensions.closeKeyboard
import com.example.healthapp.R
import com.example.healthapp.adapter.ReplyAdapter
import com.example.healthapp.data.Reply
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class ReplyFragment : Fragment (R.layout.fragment_reply) {
    private val db = FirebaseFirestore.getInstance()
    var quesRef = db.collection("Questions")
    lateinit var replyAdapter : ReplyAdapter
    lateinit var recyclerViewReply : RecyclerView
    lateinit var editTextReply : EditText
    lateinit var textViewQuestion : TextView
    lateinit var textViewDate : TextView
    lateinit var textViewUser : TextView
    lateinit var buttonReply : ImageButton
    lateinit var progressBar: ProgressBar
    lateinit var questionUid : String


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView: View = inflater.inflate(R.layout.fragment_reply, container, false)
        recyclerViewReply = rootView.findViewById(R.id.recycler_view_replies)
        editTextReply = rootView.findViewById(R.id.edit_text_reply)
        textViewQuestion = rootView.findViewById(R.id.text_view_question)
        textViewUser = rootView.findViewById(R.id.text_view_user_reply)
        textViewDate = rootView.findViewById(R.id.text_view_date_reply)
        buttonReply= rootView.findViewById(R.id.button_reply)
        progressBar = rootView.findViewById(R.id.progressBar)


        progressBar.visibility = View.VISIBLE
        val user = FirebaseAuth.getInstance().currentUser
        val bundle = arguments
        val id = bundle!!.getString("id")
        if (id != null) {
            quesRef.document(id).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val question = documentSnapshot.getString("question")
                        val date = documentSnapshot.getString("date")
                        val time = documentSnapshot.getString("time")
                        val username = documentSnapshot.getString("username")
                        val dateFormat1 = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        val currentDate = dateFormat1.format(Date())
                        if(currentDate == date)
                            textViewDate.text = time
                        else
                            textViewDate.text = date
                        textViewQuestion.text = question

                        questionUid = documentSnapshot.getString("uid").toString()

                        if(questionUid == user!!.uid){
                            textViewUser.text = "Me"
                        }
                        else {
                            textViewUser.text = username
                        }
                    } else {
                        Toast.makeText(context, "Document does not exist", Toast.LENGTH_SHORT)
                            .show()
                    }
                    progressBar.visibility = View.GONE
                }
                .addOnFailureListener(OnFailureListener { e ->
                    Toast.makeText(context, "Error!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, e.toString())
                })
            setUpRecyclerView(id)
        }

        buttonReply.setOnClickListener(View.OnClickListener {
            if(editTextReply.text.isEmpty()){
                Toast.makeText(rootView.context, "Type something first", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            else {
                Firebase.firestore.collection("users").document(user!!.uid).get()
                    .addOnCompleteListener { task ->
                        val doc = task.result
                        if (doc != null && doc.exists()) {
                            val username = doc.getString("username").toString()
                            val reply = editTextReply.text.toString()
                            if (id != null) {
                                replyToQuestion(id, reply, username, user!!.uid)
                            }
                        }
                    }
            }
        })
        return rootView
        }

    private fun setUpRecyclerView(id : String) {
        val queryReplies = quesRef.document(id).collection("Replies").orderBy("currentDate", Query.Direction.ASCENDING)
        val optionsReplies: FirestoreRecyclerOptions<Reply> = FirestoreRecyclerOptions.Builder<Reply>().setQuery(queryReplies, Reply::class.java).build()
        replyAdapter = ReplyAdapter(optionsReplies)
        recyclerViewReply.setHasFixedSize(true)
        recyclerViewReply.layoutManager = LinearLayoutManager(recyclerViewReply.context)
        recyclerViewReply.adapter = replyAdapter
    }

    private fun replyToQuestion(id : String, reply : String, username : String, uid : String){
        val dateFormat = SimpleDateFormat(
            "d MMM yyyy HH.mm.ss",
            Locale.getDefault()
        )
        val dateFormat1 = SimpleDateFormat("d MMM yyyy")
        val dateFormat2 = SimpleDateFormat("HH.mm")
        val currentDate = dateFormat1.format(Date())
        val currentTime = dateFormat2.format(Date())
        val currentDatetime = dateFormat.format(Date())
        val replyModel = Reply(id, reply, username, uid, currentDate, currentTime, currentDatetime)
        quesRef.document(id).collection("Replies").add(replyModel).addOnSuccessListener {
            editTextReply.text = null
//            Firebase.firestore.collection("users").document(uid).get().addOnCompleteListener{ user ->
//                val value : Int = user.result.getLong("questionsReplied")!!.toInt()
//                Firebase.firestore.collection("users").document(uid).update("questionsReplied", value + 1)
////                if(questionUid != uid){
////                    sendNotification()
////                }
//
//            }
        }.addOnFailureListener(OnFailureListener {
            Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show()
        })
    }

//    private fun sendNotification() {
//        // Create Retrofit
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://gdsc-notifications.herokuapp.com/api/")
//            .build()
//
//        // Create Service
//        val service = retrofit.create(NotificationInterface::class.java)
//
//        // Create JSON using JSONObject
//        val jsonObject = JSONObject()
//        jsonObject.put("uid", questionUid)
//        jsonObject.put("title", "Someone replied to your question")
//        jsonObject.put("body", "Click here to check it out")
//
//        // Convert JSONObject to String
//        val jsonObjectString = jsonObject.toString()
//
//        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
//        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())
//
//        CoroutineScope(Dispatchers.IO).launch {
//            // Do the POST request and get response
//            val response = service.createPost(requestBody)
//
//            withContext(Dispatchers.Main) {
//                if (response.isSuccessful) {
//                    Log.d("My", "done")
//
//                } else {
//                    Log.e("My", response.toString())
//                }
//            }
//        }
//    }

    override fun onStart() {
        super.onStart()
        replyAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        replyAdapter.stopListening()
        closeKeyboard()
    }

}