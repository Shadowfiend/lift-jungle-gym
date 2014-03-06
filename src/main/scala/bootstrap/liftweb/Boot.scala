package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._

import com.hacklanta.rest.FileEndpoint

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    LiftRules.addToPackages("com.hacklanta")

    val entries =
      List(
        Menu.i("Home") / "index",
        Menu.i("js") / "js" / **
      )

    LiftRules.dispatch.append(FileEndpoint)

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}
