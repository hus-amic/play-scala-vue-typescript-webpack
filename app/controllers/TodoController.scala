package controllers

import javax.inject.Inject

import io.swagger.annotations._
import models.JsonFormats._
import models.{Todo, TodoRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Riccardo Sirigu on 10/08/2017.
  */
@Api(value = "/todos")
class TodoController @Inject()(cc: ControllerComponents, todoRepo: TodoRepository) extends AbstractController(cc) {

  @ApiOperation(
    value = "Find all Todos",
    response = classOf[Todo],
    responseContainer = "List"
  )
  def getAllTodos = Action.async {
    todoRepo.getAll.map { todos =>
      Ok(Json.toJson(todos))
    }
  }


  @ApiOperation(
    value = "Get a Todo",
    response = classOf[Todo]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Todo not found")
  )
  )
  def getTodo(@ApiParam(value = "The id of the Todo to fetch") todoId: BSONObjectID) = Action.async { req =>
    todoRepo.getTodo(todoId).map { maybeTodo =>
      maybeTodo.map { todo =>
        Ok(Json.toJson(todo))
      }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Get the latest Todo",
    response = classOf[Todo]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Todo not found")
  )
  )
  def getLatest = Action.async {
    todoRepo.getLatest.map {
      maybeTodo =>
        maybeTodo.map {
          todo => Ok(Json.toJson(todo))
        }.getOrElse(NotFound)
    }
  }

  @ApiOperation(
    value = "Add a new Todo to the list",
    response = classOf[Void]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid Todo format")
  )
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "The Todo to add, in Json Format", required = true, dataType = "models.Todo", paramType = "body")
  )
  )
  def createTodo() = Action.async(parse.json) { req =>
    req.body.validate[Todo].map { todo =>
      val task = todoRepo.addTodo(todo)
      task._2.map { _ =>
        Created(task._1.toString())
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Todo format")))
  }

  @ApiOperation(
    value = "Update a Todo",
    response = classOf[Todo]
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid Todo format")
  )
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "The updated Todo, in Json Format", required = true, dataType = "models.Todo", paramType = "body")
  )
  )
  def updateTodo(@ApiParam(value = "The id of the Todo to update")
                 todoId: BSONObjectID) = Action.async(parse.json) { req =>
    req.body.validate[Todo].map { todo =>
      todoRepo.updateTodo(todoId, todo).map {
        case Some(todo) => Ok(Json.toJson(todo))
        case None => NotFound
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json")))
  }

  @ApiOperation(
    value = "Delete a Todo",
    response = classOf[Todo]
  )
  def deleteTodo(@ApiParam(value = "The id of the Todo to delete") todoId: BSONObjectID) = Action.async { req =>
    todoRepo.deleteTodo(todoId).map {
      case Some(todo) => Ok(Json.toJson(todo))
      case None => NotFound
    }
  }

}
