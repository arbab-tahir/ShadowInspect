package com.shadowinspect.app.di

import com.shadowinspect.app.domain.ml.CNNLSTMModel
import com.shadowinspect.app.domain.ml.EnsembleModel
import com.shadowinspect.app.domain.ml.FeatureExtractor
import com.shadowinspect.app.domain.ml.MLModelManager
import com.shadowinspect.app.domain.ml.RandomForestModel
import com.shadowinspect.app.domain.ml.TFLiteModelLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideFeatureExtractor(@ApplicationContext context: Context): FeatureExtractor {
        return FeatureExtractor(context)
    }

    @Provides
    @Singleton
    fun provideTFLiteModelLoader(@ApplicationContext context: Context): TFLiteModelLoader {
        return TFLiteModelLoader(context)
    }

    @Provides
    @Singleton
    fun provideCNNLSTMModel(
        @ApplicationContext context: Context,
        modelLoader: TFLiteModelLoader,
        featureExtractor: FeatureExtractor
    ): CNNLSTMModel {
        return CNNLSTMModel(context, modelLoader, featureExtractor)
    }

    @Provides
    @Singleton
    fun provideRandomForestModel(
        @ApplicationContext context: Context,
        modelLoader: TFLiteModelLoader
    ): RandomForestModel {
        return RandomForestModel(context, modelLoader)
    }

    @Provides
    @Singleton
    fun provideEnsembleModel(
        cnnLstmModel: CNNLSTMModel,
        randomForestModel: RandomForestModel
    ): EnsembleModel {
        return EnsembleModel(cnnLstmModel, randomForestModel)
    }

    @Provides
    @Singleton
    fun provideMLModelManager(
        @ApplicationContext context: Context,
        cnnLstmModel: CNNLSTMModel,
        randomForestModel: RandomForestModel,
        ensembleModel: EnsembleModel
    ): MLModelManager {
        return MLModelManager(context, cnnLstmModel, randomForestModel, ensembleModel)
    }
}
