package com.quangnguyen.stackoverflowclient.data.repository;

import com.quangnguyen.stackoverflowclient.data.model.Question;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author QuangNguyen (quangctkm9207).
 */
public class QuestionRepositoryTest {

  private static final Question question1 = new Question();
  private static final Question question2 = new Question();
  private static final Question question3 = new Question();
  private List<Question> questions = Arrays.asList(question1, question2, question3);

  @Mock
  @Local
  private QuestionDataSource localDataSource;

  @Mock
  @Remote
  private QuestionDataSource remoteDataSource;

  private QuestionRepository repository;

  private TestSubscriber<List<Question>> questionsTestSubscriber;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    repository = new QuestionRepository(localDataSource, remoteDataSource);

    questionsTestSubscriber = new TestSubscriber<>();
  }

  @Test
  public void loadQuestions_ShouldReturnCache_IfItIsAvailable() {
    // Given
    repository.caches = questions;

    // When
    repository.loadQuestions(false).subscribe(questionsTestSubscriber);

    // Then
    // No interaction with local storage or remote source
    verifyZeroInteractions(localDataSource);
    verifyZeroInteractions(remoteDataSource);

    questionsTestSubscriber.assertValue(questions);
  }

  @Test
  public void loadQuestions_ShouldReturnFromLocal_IfCacheIsNotAvailable() {
    // Given
    // No cache
    repository.caches.clear();
    doReturn(Flowable.just(questions)).when(localDataSource).loadQuestions(false);
    doReturn(Flowable.just(questions)).when(remoteDataSource).loadQuestions(true);

    // When
    repository.loadQuestions(false).subscribe(questionsTestSubscriber);

    // Then
    // Loads from local storage
    verify(localDataSource).loadQuestions(false);
    // Will load from remote source if there is no local data available
    verify(remoteDataSource).loadQuestions(true);

    questionsTestSubscriber.assertValue(questions);
  }

  @Test
  public void loadQuestions_ShouldReturnFromRemote_WhenItIsRequired() {
    // Given
    repository.caches.clear();
    doReturn(Flowable.just(questions)).when(remoteDataSource).loadQuestions(true);

    // When
    repository.loadQuestions(true).subscribe(questionsTestSubscriber);

    // Then
    // Load from remote not from local storage
    verify(remoteDataSource).loadQuestions(true);
    verify(localDataSource, never()).loadQuestions(true);
    // Cache and local storage data are clear and are filled with new data
    verify(localDataSource).clearData();
    assertEquals(repository.caches, questions);
    verify(localDataSource).addQuestion(question1);
    verify(localDataSource).addQuestion(question2);
    verify(localDataSource).addQuestion(question3);

    questionsTestSubscriber.assertValue(questions);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addQuestion_ShouldThrowException() {
    repository.addQuestion(question1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clearData_ShouldThrowException() {
    repository.clearData();
  }
}
