package hu.mapro.karti

//import android.arch.persistence.room.Room
//import android.content.Context
//import android.provider.DocumentsContract
//import dagger.Component
//import dagger.Module
//import dagger.Provides
//import hu.mapro.karti.data.AppDatabase
//
///**
// * Created by maprohu on 11/25/2017.
// */
//@Module
//class AppModule(private val context: Context) {
//    @Provides
//    fun providesAppContext() = context
//
//    @Provides
//    fun providesAppDatabse(context: Context) =
//            Room.databaseBuilder(context, AppDatabase::class.java, "karti").build()
//
//    @Provides
//    fun providesRecordingDao(db: AppDatabase) = db.recordingDao()
//
//    @Provides
//    fun providesCardDao(db: AppDatabase) = db.cardDao()
//
//
//}
//
//@Component(modules = arrayOf(AppModule::class))
//interface AppComponent {
//
//}